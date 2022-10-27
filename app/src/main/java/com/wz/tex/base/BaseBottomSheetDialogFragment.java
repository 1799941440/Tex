package com.wz.tex.base;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.wz.tex.R;

public abstract class BaseBottomSheetDialogFragment<VB extends ViewBinding> extends BottomSheetDialogFragment {

    public VB binding;
    private int contentHeight;
    private float heightBias = DEFAULT_HEIGHT_BIAS;
    private static final float DEFAULT_HEIGHT_BIAS = 0.7f;
    private float shadowAlpha = DEFAULT_SHADOW_ALPHA;
    private static final float DEFAULT_SHADOW_ALPHA = 0.5f;
    protected BottomSheetBehavior<?> b;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.MyBottomSheetDialogStyle);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (contentHeight == 0)
            contentHeight = (int) (Resources.getSystem().getDisplayMetrics().heightPixels * heightBias);
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.dimAmount = shadowAlpha;
                window.setAttributes(attributes);
            }
            FrameLayout bottomSheet = dialog.getDelegate().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // 下面功能会导致部分机型底部 导航栏遮住内容
//                CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomSheet.getLayoutParams();
//                layoutParams.height = contentHeight;
//                bottomSheet.setLayoutParams(layoutParams);
                b = BottomSheetBehavior.from(bottomSheet);
                b.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = newViewBinding(inflater, container);
        }
        return binding.getRoot();
    }

    public abstract VB newViewBinding(LayoutInflater inflater, ViewGroup container);

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
//        super.show(manager, tag);
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void dismiss() {
        dismissAllowingStateLoss();
    }

    @Override
    public void dismissAllowingStateLoss() {
        if (getFragmentManager() != null) super.dismissAllowingStateLoss();
    }

    public FragmentManager getManager() {
        FragmentActivity activity = getActivity();
        return activity == null ? null : activity.getSupportFragmentManager();
    }

    public BaseBottomSheetDialogFragment<VB> setHeightBias(float heightBias) {
        this.heightBias = heightBias;
        return this;
    }

    public BaseBottomSheetDialogFragment<VB> setShadowAlpha(float alpha) {
        this.shadowAlpha = alpha;
        return this;
    }

    public BaseBottomSheetDialogFragment<VB> setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        return this;
    }

    public void show(@NonNull FragmentManager manager) {
        String simpleName = getClass().getSimpleName();
        Fragment fragment = manager.findFragmentByTag(simpleName);
        if (fragment != null)
            manager.beginTransaction().remove(fragment).commitAllowingStateLoss();
        show(manager, simpleName);
    }
}
