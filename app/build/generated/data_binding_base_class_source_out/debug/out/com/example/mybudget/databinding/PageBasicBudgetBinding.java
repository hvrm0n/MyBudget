// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mybudget.R;
import com.google.android.material.button.MaterialButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class PageBasicBudgetBinding implements ViewBinding {
  @NonNull
  private final FrameLayout rootView;

  @NonNull
  public final MaterialButton buttonCurrencyNext;

  @NonNull
  public final EditText nameBasicBudget;

  @NonNull
  public final EditText savingsBasicBudget;

  @NonNull
  public final Spinner spinnerTypeOfBudget;

  private PageBasicBudgetBinding(@NonNull FrameLayout rootView,
      @NonNull MaterialButton buttonCurrencyNext, @NonNull EditText nameBasicBudget,
      @NonNull EditText savingsBasicBudget, @NonNull Spinner spinnerTypeOfBudget) {
    this.rootView = rootView;
    this.buttonCurrencyNext = buttonCurrencyNext;
    this.nameBasicBudget = nameBasicBudget;
    this.savingsBasicBudget = savingsBasicBudget;
    this.spinnerTypeOfBudget = spinnerTypeOfBudget;
  }

  @Override
  @NonNull
  public FrameLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static PageBasicBudgetBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static PageBasicBudgetBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.page_basic_budget, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static PageBasicBudgetBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.buttonCurrencyNext;
      MaterialButton buttonCurrencyNext = ViewBindings.findChildViewById(rootView, id);
      if (buttonCurrencyNext == null) {
        break missingId;
      }

      id = R.id.nameBasicBudget;
      EditText nameBasicBudget = ViewBindings.findChildViewById(rootView, id);
      if (nameBasicBudget == null) {
        break missingId;
      }

      id = R.id.savingsBasicBudget;
      EditText savingsBasicBudget = ViewBindings.findChildViewById(rootView, id);
      if (savingsBasicBudget == null) {
        break missingId;
      }

      id = R.id.spinnerTypeOfBudget;
      Spinner spinnerTypeOfBudget = ViewBindings.findChildViewById(rootView, id);
      if (spinnerTypeOfBudget == null) {
        break missingId;
      }

      return new PageBasicBudgetBinding((FrameLayout) rootView, buttonCurrencyNext, nameBasicBudget,
          savingsBasicBudget, spinnerTypeOfBudget);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
