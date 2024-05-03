// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
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

public final class CardAddBudgetBinding implements ViewBinding {
  @NonNull
  private final CardView rootView;

  @NonNull
  public final TextView amountAddBudget;

  @NonNull
  public final EditText amountNew;

  @NonNull
  public final CheckBox checkBoxBasic;

  @NonNull
  public final TextView currencyNewBudget;

  @NonNull
  public final EditText nameBudgetNew;

  @NonNull
  public final TextView titleAddBudget;

  @NonNull
  public final TextView typeAddBudget;

  @NonNull
  public final Spinner typeNewBudget;

  private CardAddBudgetBinding(@NonNull CardView rootView, @NonNull TextView amountAddBudget,
      @NonNull EditText amountNew, @NonNull CheckBox checkBoxBasic,
      @NonNull TextView currencyNewBudget, @NonNull EditText nameBudgetNew,
      @NonNull TextView titleAddBudget, @NonNull TextView typeAddBudget,
      @NonNull Spinner typeNewBudget) {
    this.rootView = rootView;
    this.amountAddBudget = amountAddBudget;
    this.amountNew = amountNew;
    this.checkBoxBasic = checkBoxBasic;
    this.currencyNewBudget = currencyNewBudget;
    this.nameBudgetNew = nameBudgetNew;
    this.titleAddBudget = titleAddBudget;
    this.typeAddBudget = typeAddBudget;
    this.typeNewBudget = typeNewBudget;
  }

  @Override
  @NonNull
  public CardView getRoot() {
    return rootView;
  }

  @NonNull
  public static CardAddBudgetBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static CardAddBudgetBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.card_add_budget, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static CardAddBudgetBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.amountAddBudget;
      TextView amountAddBudget = ViewBindings.findChildViewById(rootView, id);
      if (amountAddBudget == null) {
        break missingId;
      }

      id = R.id.amountNew;
      EditText amountNew = ViewBindings.findChildViewById(rootView, id);
      if (amountNew == null) {
        break missingId;
      }

      id = R.id.checkBoxBasic;
      CheckBox checkBoxBasic = ViewBindings.findChildViewById(rootView, id);
      if (checkBoxBasic == null) {
        break missingId;
      }

      id = R.id.currencyNewBudget;
      TextView currencyNewBudget = ViewBindings.findChildViewById(rootView, id);
      if (currencyNewBudget == null) {
        break missingId;
      }

      id = R.id.nameBudgetNew;
      EditText nameBudgetNew = ViewBindings.findChildViewById(rootView, id);
      if (nameBudgetNew == null) {
        break missingId;
      }

      id = R.id.titleAddBudget;
      TextView titleAddBudget = ViewBindings.findChildViewById(rootView, id);
      if (titleAddBudget == null) {
        break missingId;
      }

      id = R.id.typeAddBudget;
      TextView typeAddBudget = ViewBindings.findChildViewById(rootView, id);
      if (typeAddBudget == null) {
        break missingId;
      }

      id = R.id.typeNewBudget;
      Spinner typeNewBudget = ViewBindings.findChildViewById(rootView, id);
      if (typeNewBudget == null) {
        break missingId;
      }

      return new CardAddBudgetBinding((CardView) rootView, amountAddBudget, amountNew,
          checkBoxBasic, currencyNewBudget, nameBudgetNew, titleAddBudget, typeAddBudget,
          typeNewBudget);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}