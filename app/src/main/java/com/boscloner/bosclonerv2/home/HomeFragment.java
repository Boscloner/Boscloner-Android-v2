package com.boscloner.bosclonerv2.home;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.boscloner.bosclonerv2.Constants;
import com.boscloner.bosclonerv2.R;
import com.boscloner.bosclonerv2.SharedViewModel;
import com.boscloner.bosclonerv2.databinding.FragmentHomeBinding;
import com.boscloner.bosclonerv2.di.Injectable;

import javax.inject.Inject;

public class HomeFragment extends Fragment implements Injectable {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    FragmentHomeBinding dataBinding;
    private HomeViewModel viewModel;
    private SharedViewModel sharedViewModel;
    private HomeAdapter homeAdapter;

    public static Fragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false);
        RecyclerView recyclerView = dataBinding.list;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(dataBinding.getRoot().getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        homeAdapter = new HomeAdapter();
        homeAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                linearLayoutManager.smoothScrollToPosition(recyclerView, null, homeAdapter.getItemCount());
            }
        });
        recyclerView.setAdapter(homeAdapter);
        return dataBinding.getRoot();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HomeViewModel.class);
        dataBinding.setViewModel(viewModel);
        setupAutoCloneSwitch();
        setupWriteButton();
        sharedViewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(SharedViewModel.class);
        viewModel.getEvents().observe(this, events -> {
            if (events != null) {
                homeAdapter.setEvents(events);
            }
        });
        sharedViewModel.getConnectionStateMutableLiveData().observe(this, events -> {
            if (events != null) {
                viewModel.setConnectionStateProblem(events);
            }
        });
    }

    private void setupWriteButton() {
        dataBinding.textViewFragmentHomeIconCreate.setOnClickListener(v -> sharedViewModel.onCustomWriteClick());
    }

    private void setupAutoCloneSwitch() {
        if (getContext() != null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean autoClone = settings.getBoolean(Constants.Preferences.AUTO_CLONE_KEY, false);

            Switch autoCloneSwitch = dataBinding.switchAutoFragmentHomeClone;
            autoCloneSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (sharedViewModel != null) {
                    sharedViewModel.onAutoCloneClicked(isChecked);
                }
            });
            autoCloneSwitch.setChecked(autoClone);
        }
    }
}