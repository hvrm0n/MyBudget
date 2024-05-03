// Generated by view binder compiler. Do not edit!
package com.example.mybudget.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.mybudget.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class CardIconChooserBinding implements ViewBinding {
  @NonNull
  private final CardView rootView;

  @NonNull
  public final RecyclerView recyclerIconChooser;

  private CardIconChooserBinding(@NonNull CardView rootView,
      @NonNull RecyclerView recyclerIconChooser) {
    this.rootView = rootView;
    this.recyclerIconChooser = recyclerIconChooser;
  }

  @Override
  @NonNull
  public CardView getRoot() {
    return rootView;
  }

  @NonNull
  public static CardIconChooserBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static CardIconChooserBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.card_icon_chooser, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static CardIconChooserBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.recyclerIconChooser;
      RecyclerView recyclerIconChooser = ViewBindings.findChildViewById(rootView, id);
      if (recyclerIconChooser == null) {
        break missingId;
      }

      return new CardIconChooserBinding((CardView) rootView, recyclerIconChooser);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
