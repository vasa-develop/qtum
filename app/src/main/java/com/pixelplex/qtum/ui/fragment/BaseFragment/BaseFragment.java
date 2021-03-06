package com.pixelplex.qtum.ui.fragment.BaseFragment;

import android.app.Activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.pixelplex.qtum.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public abstract class BaseFragment extends Fragment implements BaseFragmentView {

    protected abstract void createPresenter();

    protected abstract BaseFragmentPresenterImpl getPresenter();

    protected abstract int getLayout();

    public static final String BACK_STACK_ROOT_TAG = "root_fragment";

    private Unbinder mUnbinder;
    ProgressDialog mProgressDialog;
    AlertDialog mAlertDialog;

    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Override
    public void onResume() {
        super.onResume();
        getPresenter().onResume(getActivity());
    }

    @Override
    public void setProgressDialog(String message) {
        mProgressDialog =  new ProgressDialog(getActivity());
        mProgressDialog.setTitle(message);
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        mProgressDialog.dismiss();
    }

    @Override
    public void setAlertDialog(String message) {
        //TODO: change icon and message
        mAlertDialog = new AlertDialog
                .Builder(getContext())
                .setTitle(message)
                .setMessage("TestText")
                .create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();
    }

    @Override
    public void dismissAlertDialog() {
        mAlertDialog.dismiss();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyBoard();
        getPresenter().onPause(getActivity());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPresenter();
        getPresenter().onCreate(getActivity());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindView(view);
        getPresenter().initializeViews();
        getPresenter().getView().setSoftMode();
        getPresenter().onViewCreated();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayout(), container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        getPresenter().onStart(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        getPresenter().onStop(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPresenter().onDestroyView();
        unBindView();
    }

    @Override
    public void showSoftInput() {
        ((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void setFocusTextInput(View textInputEditText, View textInputLayout){
        textInputEditText.setFocusableInTouchMode(true);
        textInputEditText.requestFocus();
        textInputLayout.setFocusableInTouchMode(true);
        textInputLayout.requestFocus();
    }

    @Override
    public void finish() {
        ActivityCompat.finishAffinity(getActivity());
    }

    @Override
    public void startActivity(Intent intent) {

    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {

    }

    @Override
    public void hideKeyBoard() {
        Activity activity = getActivity();
        View view = activity.getCurrentFocus();
        if (view != null) {
            hideKeyBoard(activity, view);
        }
    }

    public void hideKeyBoard(Activity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    @Override
    public void hideKeyBoard(View v) {

    }

    @Override
    public void openRootFragment(Fragment fragment) {
        getFragmentManager().popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        hideKeyBoard();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, fragment.getClass().getCanonicalName())
                .addToBackStack(BaseFragment.BACK_STACK_ROOT_TAG)
                .commit();
    }


    @Override
    public void openFragment(Fragment fragment) {
        hideKeyBoard();
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right,R.anim.exit_to_left,R.anim.enter_from_left,R.anim.exit_to_right)
                .add(R.id.fragment_container, fragment, fragment.getClass().getCanonicalName())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void initializeViews() {
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (null != mToolbar) {
            activity.setSupportActionBar(mToolbar);
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false);
            }
        }
    }

    @Override
    public Activity getFragmentActivity() {
        return getActivity();
    }

    protected void bindView(View view) {
        mUnbinder = ButterKnife.bind(this, view);
    }

    protected void unBindView() {
        mUnbinder.unbind();
    }

    @Override
    public void setSoftMode() {

    }
}
