// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mybudget.R;
import com.google.android.material.button.MaterialButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class PageChooseCurrencyBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final MaterialButton buttonCurrencyNext;

  @NonNull
  public final ListView listViewBasicCurrencyStart;

  @NonNull
  public final SearchView searchCurrencyBegin;

  private PageChooseCurrencyBinding(@NonNull LinearLayout rootView,
      @NonNull MaterialButton buttonCurrencyNext, @NonNull ListView listViewBasicCurrencyStart,
      @NonNull SearchView searchCurrencyBegin) {
    this.rootView = rootView;
    this.buttonCurrencyNext = buttonCurrencyNext;
    this.listViewBasicCurrencyStart = listViewBasicCurrencyStart;
    this.searchCurrencyBegin = searchCurrencyBegin;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static PageChooseCurrencyBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static PageChooseCurrencyBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.page_choose_currency, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static PageChooseCurrencyBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.buttonCurrencyNext;
      MaterialButton buttonCurrencyNext = ViewBindings.findChildViewById(rootView, id);
      if (buttonCurrencyNext == null) {
        break missingId;
      }

      id = R.id.listViewBasicCurrencyStart;
      ListView listViewBasicCurrencyStart = ViewBindings.findChildViewById(rootView, id);
      if (listViewBasicCurrencyStart == null) {
        break missingId;
      }

      id = R.id.searchCurrencyBegin;
      SearchView searchCurrencyBegin = ViewBindings.findChildViewById(rootView, id);
      if (searchCurrencyBegin == null) {
        break missingId;
      }

      return new PageChooseCurrencyBinding((LinearLayout) rootView, buttonCurrencyNext,
          listViewBasicCurrencyStart, searchCurrencyBegin);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}