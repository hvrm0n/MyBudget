// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mybudget.R;
import com.google.android.material.button.MaterialButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class PageNewGlsBinding implements ViewBinding {
  @NonNull
  private final FrameLayout rootView;

  @NonNull
  public final TextView billingPeriodGLS;

  @NonNull
  public final TextView billingPeriodTitleGLS;

  @NonNull
  public final TextView budgetGLS;

  @NonNull
  public final MaterialButton buttonAddGLS;

  @NonNull
  public final CalendarView calendarViewGLS;

  @NonNull
  public final TextView currencyGLS;

  @NonNull
  public final TextView glsDate;

  @NonNull
  public final EditText glsValue;

  @NonNull
  public final TextView iconChooseGLS;

  @NonNull
  public final ImageView imageOfGLM;

  @NonNull
  public final LinearLayout linearLayout;

  @NonNull
  public final TextView nameGLS;

  @NonNull
  public final EditText nameGLSEdit;

  @NonNull
  public final TextView periodOfLoan;

  @NonNull
  public final Spinner periodOfNotificationGLS;

  @NonNull
  public final TextView periodTitleGLS;

  @NonNull
  public final RadioGroup radioGroupGLS;

  @NonNull
  public final NestedScrollView scw;

  @NonNull
  public final Spinner spinnerBudgetGLS;

  @NonNull
  public final TextView timeOfNotificationsGLS;

  @NonNull
  public final TextView timeTitleGLS;

  @NonNull
  public final RadioButton withDate;

  @NonNull
  public final RadioButton withouthDate;

  private PageNewGlsBinding(@NonNull FrameLayout rootView, @NonNull TextView billingPeriodGLS,
      @NonNull TextView billingPeriodTitleGLS, @NonNull TextView budgetGLS,
      @NonNull MaterialButton buttonAddGLS, @NonNull CalendarView calendarViewGLS,
      @NonNull TextView currencyGLS, @NonNull TextView glsDate, @NonNull EditText glsValue,
      @NonNull TextView iconChooseGLS, @NonNull ImageView imageOfGLM,
      @NonNull LinearLayout linearLayout, @NonNull TextView nameGLS, @NonNull EditText nameGLSEdit,
      @NonNull TextView periodOfLoan, @NonNull Spinner periodOfNotificationGLS,
      @NonNull TextView periodTitleGLS, @NonNull RadioGroup radioGroupGLS,
      @NonNull NestedScrollView scw, @NonNull Spinner spinnerBudgetGLS,
      @NonNull TextView timeOfNotificationsGLS, @NonNull TextView timeTitleGLS,
      @NonNull RadioButton withDate, @NonNull RadioButton withouthDate) {
    this.rootView = rootView;
    this.billingPeriodGLS = billingPeriodGLS;
    this.billingPeriodTitleGLS = billingPeriodTitleGLS;
    this.budgetGLS = budgetGLS;
    this.buttonAddGLS = buttonAddGLS;
    this.calendarViewGLS = calendarViewGLS;
    this.currencyGLS = currencyGLS;
    this.glsDate = glsDate;
    this.glsValue = glsValue;
    this.iconChooseGLS = iconChooseGLS;
    this.imageOfGLM = imageOfGLM;
    this.linearLayout = linearLayout;
    this.nameGLS = nameGLS;
    this.nameGLSEdit = nameGLSEdit;
    this.periodOfLoan = periodOfLoan;
    this.periodOfNotificationGLS = periodOfNotificationGLS;
    this.periodTitleGLS = periodTitleGLS;
    this.radioGroupGLS = radioGroupGLS;
    this.scw = scw;
    this.spinnerBudgetGLS = spinnerBudgetGLS;
    this.timeOfNotificationsGLS = timeOfNotificationsGLS;
    this.timeTitleGLS = timeTitleGLS;
    this.withDate = withDate;
    this.withouthDate = withouthDate;
  }

  @Override
  @NonNull
  public FrameLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static PageNewGlsBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static PageNewGlsBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.page_new_gls, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static PageNewGlsBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.billingPeriodGLS;
      TextView billingPeriodGLS = ViewBindings.findChildViewById(rootView, id);
      if (billingPeriodGLS == null) {
        break missingId;
      }

      id = R.id.billingPeriodTitleGLS;
      TextView billingPeriodTitleGLS = ViewBindings.findChildViewById(rootView, id);
      if (billingPeriodTitleGLS == null) {
        break missingId;
      }

      id = R.id.budgetGLS;
      TextView budgetGLS = ViewBindings.findChildViewById(rootView, id);
      if (budgetGLS == null) {
        break missingId;
      }

      id = R.id.buttonAddGLS;
      MaterialButton buttonAddGLS = ViewBindings.findChildViewById(rootView, id);
      if (buttonAddGLS == null) {
        break missingId;
      }

      id = R.id.calendarViewGLS;
      CalendarView calendarViewGLS = ViewBindings.findChildViewById(rootView, id);
      if (calendarViewGLS == null) {
        break missingId;
      }

      id = R.id.currencyGLS;
      TextView currencyGLS = ViewBindings.findChildViewById(rootView, id);
      if (currencyGLS == null) {
        break missingId;
      }

      id = R.id.glsDate;
      TextView glsDate = ViewBindings.findChildViewById(rootView, id);
      if (glsDate == null) {
        break missingId;
      }

      id = R.id.glsValue;
      EditText glsValue = ViewBindings.findChildViewById(rootView, id);
      if (glsValue == null) {
        break missingId;
      }

      id = R.id.iconChooseGLS;
      TextView iconChooseGLS = ViewBindings.findChildViewById(rootView, id);
      if (iconChooseGLS == null) {
        break missingId;
      }

      id = R.id.imageOfGLM;
      ImageView imageOfGLM = ViewBindings.findChildViewById(rootView, id);
      if (imageOfGLM == null) {
        break missingId;
      }

      id = R.id.linearLayout;
      LinearLayout linearLayout = ViewBindings.findChildViewById(rootView, id);
      if (linearLayout == null) {
        break missingId;
      }

      id = R.id.nameGLS;
      TextView nameGLS = ViewBindings.findChildViewById(rootView, id);
      if (nameGLS == null) {
        break missingId;
      }

      id = R.id.nameGLSEdit;
      EditText nameGLSEdit = ViewBindings.findChildViewById(rootView, id);
      if (nameGLSEdit == null) {
        break missingId;
      }

      id = R.id.periodOfLoan;
      TextView periodOfLoan = ViewBindings.findChildViewById(rootView, id);
      if (periodOfLoan == null) {
        break missingId;
      }

      id = R.id.periodOfNotificationGLS;
      Spinner periodOfNotificationGLS = ViewBindings.findChildViewById(rootView, id);
      if (periodOfNotificationGLS == null) {
        break missingId;
      }

      id = R.id.periodTitleGLS;
      TextView periodTitleGLS = ViewBindings.findChildViewById(rootView, id);
      if (periodTitleGLS == null) {
        break missingId;
      }

      id = R.id.radioGroupGLS;
      RadioGroup radioGroupGLS = ViewBindings.findChildViewById(rootView, id);
      if (radioGroupGLS == null) {
        break missingId;
      }

      id = R.id.scw;
      NestedScrollView scw = ViewBindings.findChildViewById(rootView, id);
      if (scw == null) {
        break missingId;
      }

      id = R.id.spinnerBudgetGLS;
      Spinner spinnerBudgetGLS = ViewBindings.findChildViewById(rootView, id);
      if (spinnerBudgetGLS == null) {
        break missingId;
      }

      id = R.id.timeOfNotificationsGLS;
      TextView timeOfNotificationsGLS = ViewBindings.findChildViewById(rootView, id);
      if (timeOfNotificationsGLS == null) {
        break missingId;
      }

      id = R.id.timeTitleGLS;
      TextView timeTitleGLS = ViewBindings.findChildViewById(rootView, id);
      if (timeTitleGLS == null) {
        break missingId;
      }

      id = R.id.withDate;
      RadioButton withDate = ViewBindings.findChildViewById(rootView, id);
      if (withDate == null) {
        break missingId;
      }

      id = R.id.withouthDate;
      RadioButton withouthDate = ViewBindings.findChildViewById(rootView, id);
      if (withouthDate == null) {
        break missingId;
      }

      return new PageNewGlsBinding((FrameLayout) rootView, billingPeriodGLS, billingPeriodTitleGLS,
          budgetGLS, buttonAddGLS, calendarViewGLS, currencyGLS, glsDate, glsValue, iconChooseGLS,
          imageOfGLM, linearLayout, nameGLS, nameGLSEdit, periodOfLoan, periodOfNotificationGLS,
          periodTitleGLS, radioGroupGLS, scw, spinnerBudgetGLS, timeOfNotificationsGLS,
          timeTitleGLS, withDate, withouthDate);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
