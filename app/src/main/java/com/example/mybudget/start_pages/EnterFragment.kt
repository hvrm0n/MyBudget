package com.example.mybudget.start_pages

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.Navigation
import com.example.mybudget.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class EnterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInButton: SignInButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.choose, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Вход и регистрация по почте и паролю
        view.findViewById<Button>(R.id.SignUp).setOnClickListener(Navigation.createNavigateOnClickListener(
            R.id.action_enterFragment_to_signUpFragment
        ))
        view.findViewById<Button>(R.id.LogIn).setOnClickListener(Navigation.createNavigateOnClickListener(
            R.id.action_enterFragment_to_logInFragment
        ))
        signInButton = view.findViewById(R.id.sign_in_button_google)
    }

    override fun onStart() {
        super.onStart()

        auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null){
            Navigation.findNavController(requireView()).navigate(R.id.action_enterFragment_to_homePageActivity)
            requireActivity().finish()
        }

        //Вход и регистрация с использованием аккаунта Google
        signInButton.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("480721209154-7s727dt7jqmnvlsvqqss5grs0h0eg5q0.apps.googleusercontent.com")
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignIn()
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode ==  Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            manageResults(task)
        } else Log.e(Constants.TAG_GOOGLE, result.resultCode.toString())
    }

    private fun googleSignIn(){
        val signInClient = googleSignInClient.signInIntent
        launcher.launch(signInClient)
    }

    private fun manageResults(task: Task<GoogleSignInAccount>){
        val account: GoogleSignInAccount? = task.result
        if (account!=null){
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener {
                if (task.isSuccessful){
                    Navigation.findNavController(requireView()).navigate(R.id.action_logInFragment_to_homePageActivity)
                    requireActivity().finish()
                } else {
                    Log.e(Constants.TAG_GOOGLE, task.exception.toString())
                    Toast.makeText(requireActivity(), "Не удалось войти в аккаунт!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else{
            Log.e(Constants.TAG_GOOGLE, task.exception.toString())
            Toast.makeText(requireActivity(), "Не удалось войти в аккаунт!", Toast.LENGTH_SHORT).show()
        }
    }
}