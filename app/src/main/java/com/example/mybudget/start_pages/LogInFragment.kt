package com.example.mybudget.start_pages

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.mybudget.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LogInFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var hintEmail: TextView
    private lateinit var hintPassword: TextView
    private lateinit var logInButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.log_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProvider(this)[LogInViewModel::class.java]

        email = view.findViewById(R.id.email_edittext_login)
        hintEmail = view.findViewById(R.id.hintEmailLogIn)
        password = view.findViewById(R.id.password_edittext_login)
        hintPassword = view.findViewById(R.id.hintPasswordLogIn)
        logInButton = view.findViewById(R.id.buttonLogIn)

        viewModel.emailText.value?.let {
            email.setText(it)
        }

        viewModel.passwordText.value?.let {
            password.setText(it)
        }

        viewModel.hintEmailState.value?.let {

            hintEmail.visibility = if (it == 0) TextView.VISIBLE else TextView.GONE
        }

        viewModel.hintPasswordState.value?.let {
            hintPassword.visibility = if (it == 0) TextView.VISIBLE else TextView.GONE
        }

        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

        email.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (!email.text.toString().matches(emailPattern.toRegex())) hintEmail.visibility = TextView.VISIBLE
                else logInButton.isEnabled = password.text.length >= 6
            } else hintEmail.visibility = TextView.GONE

            viewModel.hintEmailState.value = hintEmail.visibility
        }


        password.setOnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (password.text.length<6) hintPassword.visibility = TextView.VISIBLE
                else logInButton.isEnabled = email.text.toString().matches(emailPattern.toRegex())
            } else hintPassword.visibility = TextView.GONE

            viewModel.hintPasswordState.value = hintPassword.visibility
        }

        email.doAfterTextChanged {
            viewModel.emailText.value = it.toString()
            logInButton.isEnabled = email.text.toString().matches(emailPattern.toRegex()) && password.text.length >= 6
        }

        password.doAfterTextChanged {
            viewModel.passwordText.value = it.toString()
            logInButton.isEnabled = email.text.toString().matches(emailPattern.toRegex()) && password.text.length >= 6
        }

    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth

        logInButton.setOnClickListener { logIn() }
    }

    private fun logIn(){
        auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString()).addOnCompleteListener { task->
            if (!task.isSuccessful) {
                Snackbar.make(email, "Такой учетной записи не существует!", Snackbar.LENGTH_LONG).show()
            } else updateUI()
        }
    }

    private fun updateUI(){
        Navigation.findNavController(requireView()).navigate(R.id.action_logInFragment_to_homePageActivity)
        password.text.clear()
        email.text.clear()
        activity?.finishAffinity()
    }

}