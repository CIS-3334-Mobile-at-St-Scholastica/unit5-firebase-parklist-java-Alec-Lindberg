package cis3334.java_firebase_parklist.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import cis3334.java_firebase_parklist.R; // Ensure this is correctly imported
import cis3334.java_firebase_parklist.data.model.Park;
import cis3334.java_firebase_parklist.navigation.AppDestinations; // Assuming you'll use this for arg keys
import cis3334.java_firebase_parklist.viewmodel.ParkViewModel; // We will create this Java ViewModel later
import java.util.Locale;

public class ParkListFragment extends Fragment {

    private ParkViewModel parkViewModel;
    private RecyclerView recyclerViewParks;
    private ParkAdapter parkAdapter;
    private TextView textViewParkCount;
    private TextView textViewEmptyList;
    private NavController navController;

    public ParkListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModel - scoped to the Activity
        parkViewModel = new ViewModelProvider(requireActivity()).get(ParkViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_park_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        recyclerViewParks = view.findViewById(R.id.recyclerViewParks);
        textViewParkCount = view.findViewById(R.id.textViewParkCount);
        textViewEmptyList = view.findViewById(R.id.textViewEmptyList);
        FloatingActionButton fabAddPark = view.findViewById(R.id.fabAddPark);

        // Setup RecyclerView
        recyclerViewParks.setLayoutManager(new LinearLayoutManager(getContext()));
        parkAdapter = new ParkAdapter(park -> {
            // Handle park click: Navigate to ParkDetailFragment
            Log.d("ParkListFragment", "Park clicked: " + park.getName());
            Bundle args = new Bundle();
            args.putString(AppDestinations.PARK_ID_ARG, park.getId());
            // Make sure the action ID in nav_graph.xml matches
            navController.navigate(R.id.action_parkListFragment_to_parkDetailFragment, args);
        });
        recyclerViewParks.setAdapter(parkAdapter);

        // Observe LiveData from ViewModel
        parkViewModel.getParks().observe(getViewLifecycleOwner(), parks -> {
            Log.d("ParkListFragment", "Park list updated. Size: " + parks.size());
            parkAdapter.submitList(parks); // ListAdapter method
            textViewParkCount.setText(String.format(Locale.getDefault(), "Number of parks: %d", parks.size()));
            if (parks.isEmpty()) {
                textViewEmptyList.setVisibility(View.VISIBLE);
                recyclerViewParks.setVisibility(View.GONE);
            } else {
                textViewEmptyList.setVisibility(View.GONE);
                recyclerViewParks.setVisibility(View.VISIBLE);
            }
        });

        // FAB click listener
        fabAddPark.setOnClickListener(v -> {
            Log.d("ParkListFragment", "FAB clicked");
            // Make sure the action ID in nav_graph.xml matches
             navController.navigate(R.id.action_parkListFragment_to_addParkFragment);
        });
        parkViewModel.loadParks();
        // If you used a TopAppBar in XML, you might want to set it up here if it needs dynamic content
        // or if it's not part of the activity's main toolbar.
        // For simplicity, the MaterialToolbar in fragment_park_list.xml handles the title.
    }
}
