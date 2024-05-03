// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mybudget.R;
import com.google.android.gms.common.SignInButton;
import com.google.android.material.button.MaterialButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ChooseBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final MaterialButton LogIn;

  @NonNull
  public final MaterialButton SignUp;

  @NonNull
  public final TextView infoClose;

  @NonNull
  public final ImageView keepFinanceImage;

  @NonNull
  public final TextView or;

  @NonNull
  public final TextView reasonText;

  @NonNull
  public final SignInButton signInButtonGoogle;

  @NonNull
  public final Space space;

  private ChooseBinding(@NonNull ConstraintLayout rootView, @NonNull MaterialButton LogIn,
      @NonNull MaterialButton SignUp, @NonNull TextView infoClose,
      @NonNull ImageView keepFinanceImage, @NonNull TextView or, @NonNull TextView reasonText,
      @NonNull SignInButton signInButtonGoogle, @NonNull Space space) {
    this.rootView = rootView;
    this.LogIn = LogIn;
    this.SignUp = SignUp;
    this.infoClose = infoClose;
    this.keepFinanceImage = keepFinanceImage;
    this.or = or;
    this.reasonText = reasonText;
    this.signInButtonGoogle = signInButtonGoogle;
    this.space = space;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ChooseBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ChooseBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
      boolean attachToParent) {
    View root = inflater.inflate(R.layout.choose, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ChooseBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.LogIn;
      MaterialButton LogIn = ViewBindings.findChildViewById(rootView, id);
      if (LogIn == null) {
        break missingId;
      }

      id = R.id.SignUp;
      MaterialButton SignUp = ViewBindings.findChildViewById(rootView, id);
      if (SignUp == null) {
        break missingId;
      }

      id = R.id.infoClose;
      TextView infoClose = ViewBindings.findChildViewById(rootView, id);
      if (infoClose == null) {
        break missingId;
      }

      id = R.id.keepFinanceImage;
      ImageView keepFinanceImage = ViewBindings.findChildViewById(rootView, id);
      if (keepFinanceImage == null) {
        break missingId;
      }

      id = R.id.or;
      TextView or = ViewBindings.findChildViewById(rootView, id);
      if (or == null) {
        break missingId;
      }

      id = R.id.reasonText;
      TextView reasonText = ViewBindings.findChildViewById(rootView, id);
      if (reasonText == null) {
        break missingId;
      }

      id = R.id.sign_in_button_google;
      SignInButton signInButtonGoogle = ViewBindings.findChildViewById(rootView, id);
      if (signInButtonGoogle == null) {
        break missingId;
      }

      id = R.id.space;
      Space space = ViewBindings.findChildViewById(rootView, id);
      if (space == null) {
        break missingId;
      }

      return new ChooseBinding((ConstraintLayout) rootView, LogIn, SignUp, infoClose,
          keepFinanceImage, or, reasonText, signInButtonGoogle, space);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
