// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import androidx.viewpager2.widget.ViewPager2;
import com.example.mybudget.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class PageFinanceBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final RecyclerView budgetsList;

  @NonNull
  public final TextView calculate;

  @NonNull
  public final RecyclerView categoryList;

  @NonNull
  public final FloatingActionButton fabCalculate;

  @NonNull
  public final FloatingActionButton fabHistory;

  @NonNull
  public final FloatingActionButton fabNewTransaction;

  @NonNull
  public final FloatingActionButton floatingActionButton;

  @NonNull
  public final TextView history;

  @NonNull
  public final ConstraintLayout layoutFinance;

  @NonNull
  public final ImageButton leftNav;

  @NonNull
  public final LinearLayout linearLayoutFinance;

  @NonNull
  public final ImageButton rightNav;

  @NonNull
  public final NestedScrollView scw;

  @NonNull
  public final TextView transaction;

  @NonNull
  public final ViewPager2 viewpager;

  private PageFinanceBinding(@NonNull ConstraintLayout rootView, @NonNull RecyclerView budgetsList,
      @NonNull TextView calculate, @NonNull RecyclerView categoryList,
      @NonNull FloatingActionButton fabCalculate, @NonNull FloatingActionButton fabHistory,
      @NonNull FloatingActionButton fabNewTransaction,
      @NonNull FloatingActionButton floatingActionButton, @NonNull TextView history,
      @NonNull ConstraintLayout layoutFinance, @NonNull ImageButton leftNav,
      @NonNull LinearLayout linearLayoutFinance, @NonNull ImageButton rightNav,
      @NonNull NestedScrollView scw, @NonNull TextView transaction, @NonNull ViewPager2 viewpager) {
    this.rootView = rootView;
    this.budgetsList = budgetsList;
    this.calculate = calculate;
    this.categoryList = categoryList;
    this.fabCalculate = fabCalculate;
    this.fabHistory = fabHistory;
    this.fabNewTransaction = fabNewTransaction;
    this.floatingActionButton = floatingActionButton;
    this.history = history;
    this.layoutFinance = layoutFinance;
    this.leftNav = leftNav;
    this.linearLayoutFinance = linearLayoutFinance;
    this.rightNav = rightNav;
    this.scw = scw;
    this.transaction = transaction;
    this.viewpager = viewpager;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static PageFinanceBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static PageFinanceBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.page_finance, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static PageFinanceBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.budgetsList;
      RecyclerView budgetsList = ViewBindings.findChildViewById(rootView, id);
      if (budgetsList == null) {
        break missingId;
      }

      id = R.id.calculate;
      TextView calculate = ViewBindings.findChildViewById(rootView, id);
      if (calculate == null) {
        break missingId;
      }

      id = R.id.categoryList;
      RecyclerView categoryList = ViewBindings.findChildViewById(rootView, id);
      if (categoryList == null) {
        break missingId;
      }

      id = R.id.fabCalculate;
      FloatingActionButton fabCalculate = ViewBindings.findChildViewById(rootView, id);
      if (fabCalculate == null) {
        break missingId;
      }

      id = R.id.fabHistory;
      FloatingActionButton fabHistory = ViewBindings.findChildViewById(rootView, id);
      if (fabHistory == null) {
        break missingId;
      }

      id = R.id.fabNewTransaction;
      FloatingActionButton fabNewTransaction = ViewBindings.findChildViewById(rootView, id);
      if (fabNewTransaction == null) {
        break missingId;
      }

      id = R.id.floatingActionButton;
      FloatingActionButton floatingActionButton = ViewBindings.findChildViewById(rootView, id);
      if (floatingActionButton == null) {
        break missingId;
      }

      id = R.id.history;
      TextView history = ViewBindings.findChildViewById(rootView, id);
      if (history == null) {
        break missingId;
      }

      ConstraintLayout layoutFinance = (ConstraintLayout) rootView;

      id = R.id.left_nav;
      ImageButton leftNav = ViewBindings.findChildViewById(rootView, id);
      if (leftNav == null) {
        break missingId;
      }

      id = R.id.linearLayoutFinance;
      LinearLayout linearLayoutFinance = ViewBindings.findChildViewById(rootView, id);
      if (linearLayoutFinance == null) {
        break missingId;
      }

      id = R.id.right_nav;
      ImageButton rightNav = ViewBindings.findChildViewById(rootView, id);
      if (rightNav == null) {
        break missingId;
      }

      id = R.id.scw;
      NestedScrollView scw = ViewBindings.findChildViewById(rootView, id);
      if (scw == null) {
        break missingId;
      }

      id = R.id.transaction;
      TextView transaction = ViewBindings.findChildViewById(rootView, id);
      if (transaction == null) {
        break missingId;
      }

      id = R.id.viewpager;
      ViewPager2 viewpager = ViewBindings.findChildViewById(rootView, id);
      if (viewpager == null) {
        break missingId;
      }

      return new PageFinanceBinding((ConstraintLayout) rootView, budgetsList, calculate,
          categoryList, fabCalculate, fabHistory, fabNewTransaction, floatingActionButton, history,
          layoutFinance, leftNav, linearLayoutFinance, rightNav, scw, transaction, viewpager);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}