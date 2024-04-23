package com.example.mybudget.start_pages

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var signUpButton: Button
    private lateinit var hintEmail: TextView
    private lateinit var hintPassword: TextView
    private lateinit var table: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProvider(this)[SignUpViewModel::class.java]

        email = view.findViewById(R.id.email_edittext_signup)
        hintEmail = view.findViewById(R.id.hintEmailSignUp)
        password = view.findViewById(R.id.password_edittext_signup)
        hintPassword = view.findViewById(R.id.hintPasswordSignUp)
        signUpButton = view.findViewById(R.id.buttonSignUp)

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
                else signUpButton.isEnabled = password.text.length >= 6
            } else hintEmail.visibility = TextView.GONE

            viewModel.hintEmailState.value = hintEmail.visibility
        }

        password.setOnFocusChangeListener{ _, hasFocus ->
            if (!hasFocus) {
                if (password.text.length<6) hintPassword.visibility = TextView.VISIBLE
                else signUpButton.isEnabled = email.text.toString().matches(emailPattern.toRegex())
            } else hintPassword.visibility = TextView.GONE

            viewModel.hintPasswordState.value = hintPassword.visibility
        }

        email.doAfterTextChanged {
            viewModel.emailText.value = it.toString()
            signUpButton.isEnabled = email.text.toString().matches(emailPattern.toRegex()) && password.text.length >= 6
        }

        password.doAfterTextChanged {
            viewModel.passwordText.value = it.toString()
            signUpButton.isEnabled = email.text.toString().matches(emailPattern.toRegex()) && password.text.length >= 6
        }
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        signUpButton.setOnClickListener { emailSignUp() }
    }

    private fun emailSignUp(){
        auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {

                    Snackbar.make(
                        password,
                        "Вы успешно зарегистрировались!",
                        Snackbar.LENGTH_SHORT,
                    ).show()
                    val categories = resources.getStringArray(R.array.category_default)
                    val paths = resources.getStringArray(R.array.icons_default)
                    for (i in categories.indices){
                        table.child("Users").child(auth.currentUser!!.uid).child("Categories").child("Categories base").push().setValue(_CategoryBegin(categories[i], paths[i]))
                    }
                    updateUI()
                } else {
                    Log.e(Constants.TAG_SIGNUP,  task.exception.toString())

                    Snackbar.make(
                        password,
                        "Не удалось создать в аккаунт, попробуйте еще раз.",
                        Snackbar.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    private fun updateUI(){
        Navigation.findNavController(requireView()).navigate(R.id.action_signUpFragment_to_currencyFragment)
        password.text.clear()
        email.text.clear()
    }
}

data class _CategoryBegin(var name:String="", var path:String="")
data class CategoryBeginWithKey(var key:String="", var categoryBegin:_CategoryBegin)