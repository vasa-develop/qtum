package com.pixelplex.qtum.ui.fragment.BackUpWalletFragment;


import com.pixelplex.qtum.ui.fragment.BaseFragment.BaseFragmentView;

interface BackUpWalletFragmentView extends BaseFragmentView {
    void setBrainCode(String seed);
    void showToast();
}
