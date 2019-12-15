package com.bitflip.potholereporting.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bitflip.potholereporting.MainActivity
import com.bitflip.potholereporting.R
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_home.view.text_loginstatus

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(this, Observer {
            textView.text = it
        })

        root.button_signin.setOnClickListener {
            startSignin()
        }

        root.button_signout.setOnClickListener {
            startSignout()
        }

        if(FirebaseAuth.getInstance().currentUser != null){
            root.text_loginstatus.text = "Welcome, You are logged in"
        }

        return root
    }

    private fun startSignin(){
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            MainActivity.RC_SIGN_IN
        )
    }

    private fun startSignout(){
        AuthUI.getInstance()
            .signOut(activity!!.applicationContext)
            .addOnCompleteListener {
                text_loginstatus.text = "Please Sign In"
                Toast.makeText(activity?.applicationContext, "Successfully Signed Out", Toast.LENGTH_SHORT).show()
            }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MainActivity.RC_SIGN_IN) {
            // val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                text_loginstatus.text = "Welcome, You are logged in"
                Toast.makeText(activity?.applicationContext, "Successfully signed in", Toast.LENGTH_SHORT).show()
            } else {
                // Sign in failed. If response is null
                Toast.makeText(activity?.applicationContext, "Some error occurred, please try again", Toast.LENGTH_SHORT).show()
                Log.d("Login", "Login Failed")
            }
        }
    }
}