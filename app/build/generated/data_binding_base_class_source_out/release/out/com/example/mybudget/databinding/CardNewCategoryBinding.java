// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mybudget.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class CardNewCategoryBinding implements ViewBinding {
  @NonNull
  private final CardView rootView;

  @NonNull
  public final EditText categoryNewValue;

  @NonNull
  public final TextView iconChooseCategory;

  @NonNull
  public final ImageView imageOfCategory;

  @NonNull
  public final Spinner spinnerProrityEdit;

  @NonNull
  public final TextView textChooseIcon;

  private CardNewCategoryBinding(@NonNull CardView rootView, @NonNull EditText categoryNewValue,
      @NonNull TextView iconChooseCategory, @NonNull ImageView imageOfCategory,
      @NonNull Spinner spinnerProrityEdit, @NonNull TextView textChooseIcon) {
    this.rootView = rootView;
    this.categoryNewValue = categoryNewValue;
    this.iconChooseCategory = iconChooseCategory;
    this.imageOfCategory = imageOfCategory;
    this.spinnerProrityEdit = spinnerProrityEdit;
    this.textChooseIcon = textChooseIcon;
  }

  @Override
  @NonNull
  public CardView getRoot() {
    return rootView;
  }

  @NonNull
  public static CardNewCategoryBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static CardNewCategoryBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.card_new_category, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static CardNewCategoryBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.categoryNewValue;
      EditText categoryNewValue = ViewBindings.findChildViewById(rootView, id);
      if (categoryNewValue == null) {
        break missingId;
      }

      id = R.id.iconChooseCategory;
      TextView iconChooseCategory = ViewBindings.findChildViewById(rootView, id);
      if (iconChooseCategory == null) {
        break missingId;
      }

      id = R.id.imageOfCategory;
      ImageView imageOfCategory = ViewBindings.findChildViewById(rootView, id);
      if (imageOfCategory == null) {
        break missingId;
      }

      id = R.id.spinnerProrityEdit;
      Spinner spinnerProrityEdit = ViewBindings.findChildViewById(rootView, id);
      if (spinnerProrityEdit == null) {
        break missingId;
      }

      id = R.id.textChooseIcon;
      TextView textChooseIcon = ViewBindings.findChildViewById(rootView, id);
      if (textChooseIcon == null) {
        break missingId;
      }

      return new CardNewCategoryBinding((CardView) rootView, categoryNewValue, iconChooseCategory,
          imageOfCategory, spinnerProrityEdit, textChooseIcon);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}