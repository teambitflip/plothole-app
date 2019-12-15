package com.bitflip.potholereporting.ui.report

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bitflip.potholereporting.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.fragment_report.view.*
import java.io.File


class ReportFragment : Fragment() {
    val CAMERA_REQUEST_CODE = 0
    private var mCurrentPhotoPath = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_report, container, false)

        if(FirebaseAuth.getInstance().currentUser != null){
            root.textview_report_loginstatus.text = "Report a Pothole"
            root.button_report_camera.visibility = View.VISIBLE
        } else {
            root.textview_report_loginstatus.text = "Not Signed in"
            root.button_report_camera.visibility = View.GONE
        }

        root.button_report_camera.setOnClickListener {
            validatePermissionsForInternalStorage()
        }

        return root
    }

    // Permission to write to Storage
    private fun validatePermissionsForInternalStorage() {
        Dexter.withActivity(activity)
            .withPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object: PermissionListener {
                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(activity?.applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
                    Toast.makeText(activity?.applicationContext, "Please provide Camera Permission to click a photo", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        AlertDialog.Builder(activity)
                            .setTitle("Storage Permission")
                            .setMessage("Pothole Reporter need access to your storage to save the images")
                            .setNegativeButton(
                                android.R.string.cancel
                            ) { dialog, _ ->
                                dialog.dismiss()
                                token?.cancelPermissionRequest()
                            }
                            .setPositiveButton(android.R.string.ok
                            ) { dialog, _ ->
                                dialog.dismiss()
                                token?.continuePermissionRequest()
                            }
                            .setOnDismissListener {
                                token?.cancelPermissionRequest() }
                            .show()
                    }
                }

                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    Log.w("Report", "Permission is granted")
                    launchCamera()
                }
            }).check()
    }

    // Take permission for camera and start camera
    private fun launchCamera() {
        Dexter.withActivity(activity)
            .withPermission(android.Manifest.permission.CAMERA)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    val values = ContentValues(1)
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")

                    val fileUri = context?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                    if(context?.packageManager?.let { intent.resolveActivity(it) } != null) {
                        mCurrentPhotoPath = fileUri.toString()

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        startActivityForResult(intent, CAMERA_REQUEST_CODE)
                    }
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(activity?.applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        AlertDialog.Builder(activity)
                            .setTitle("Camera Permission")
                            .setMessage("Pothole Reporter need access to your Camera")
                            .setNegativeButton(
                                android.R.string.cancel
                            ) { dialog, _ ->
                                dialog.dismiss()
                                token?.cancelPermissionRequest()
                            }
                            .setPositiveButton(android.R.string.ok
                            ) { dialog, _ ->
                                dialog.dismiss()
                                token?.continuePermissionRequest()
                            }
                            .setOnDismissListener {
                                token?.cancelPermissionRequest() }
                            .show()
                    }
                }
            }).check()
    }

    // When the camera activity is successfully closed
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST_CODE) {
            processCapturedPhoto()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // Save the image and upload
    private fun processCapturedPhoto() {
        val cursor = context?.contentResolver?.query(
            Uri.parse(mCurrentPhotoPath),
            Array(1) {MediaStore.Images.ImageColumns.DATA}, null, null, null)

        cursor?.moveToFirst()
        val photoPath = cursor?.getString(0)
        cursor?.close()

        val file = File(photoPath)
        uploadPhotoToFirebaseStorage(file)
    }

    // Random String Generator for the title of the image in Firebase storage
    private fun randomString(): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()"
        return (1..10).map { _ -> kotlin.random.Random.nextInt(0, charPool.length) }.map(charPool::get).joinToString("")
    }

    // Upload to Firebase storage and update db
    private fun uploadPhotoToFirebaseStorage(file: File?) {
        Toast.makeText(activity?.applicationContext, "Starting Image Upload", Toast.LENGTH_SHORT).show()
        val fileURI = Uri.fromFile(file)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val storage = FirebaseStorage.getInstance()
        val imagesRef : StorageReference? = storage.reference.child("images/${uid}/${randomString()}")

        val uploadTask = imagesRef?.putFile(fileURI)

        uploadTask?.addOnSuccessListener {
            Toast.makeText(activity?.applicationContext, "Image Uploaded successfully", Toast.LENGTH_SHORT).show()
        }?.addOnFailureListener {
            Toast.makeText(activity?.applicationContext, "Image Upload Failed", Toast.LENGTH_SHORT).show()
        }

        uploadTask?.continueWithTask {task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imagesRef.downloadUrl
        }?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the download link, get gps coordinates, update db

                val downloadUri = task.result
                getGPSCoordinatesAndUpdateDatabase(downloadUri.toString())

                // Log.d("Report", downloadUri.toString())
            } else {
                Toast.makeText(activity?.applicationContext, "Some Error Occurred getting the Download URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private fun getGPSCoordinatesAndUpdateDatabase(link: String){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!.applicationContext)

        Dexter.withActivity(activity).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    fusedLocationClient.lastLocation.addOnSuccessListener {location: Location? ->
                        val lat = location?.latitude
                        val long = location?.longitude

                        updateFirebaseDatabaseAndPrivateServer(link, "${lat.toString()},${long.toString()}")

                    }.addOnFailureListener {
                        Log.d("Report","Failed to get Location")
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        AlertDialog.Builder(activity)
                            .setTitle("Camera Permission")
                            .setMessage("Pothole Reporter need access to your Camera")
                            .setNegativeButton(
                                android.R.string.cancel
                            ) { dialog, _ ->
                                dialog.dismiss()
                                token?.cancelPermissionRequest()
                            }
                            .setPositiveButton(android.R.string.ok
                            ) { dialog, _ ->
                                dialog.dismiss()
                                token?.continuePermissionRequest()
                            }
                            .setOnDismissListener {
                                token?.cancelPermissionRequest() }
                            .show()
                    }
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(activity?.applicationContext, "Location Permission Denied", Toast.LENGTH_SHORT).show()
                }

            }).check()
    }


    private fun updateFirebaseDatabaseAndPrivateServer(link: String?, coordinates: String?){
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val databaseRef = FirebaseDatabase.getInstance().reference

        @IgnoreExtraProperties
        data class Submission(
            var image_link: String?,
            var gps_coordinates: String?,
            var processed : String?
        )

        val submissionData = Submission(link, coordinates, "false")

        val childRef = databaseRef.child("submissions").child("$uid").push()
        childRef.setValue(submissionData)

        Log.d("Report", "Data successfully written to Firebase DB")

        // pingPrivateServer(link)
    }

    // TODO : Implement this later
    // private fun pingPrivateServer(link: String){}
}