package cis3334.java_firebase_parklist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import cis3334.java_firebase_parklist.data.firebase.FirebaseService;
import cis3334.java_firebase_parklist.data.model.Park;

public class ParkViewModel extends ViewModel {

    // ---- Data source ----
    private final FirebaseService service = new FirebaseService();

    // ---- UI state ----
    private final MutableLiveData<List<Park>> parks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Park> selectedPark = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    // Expose as immutable LiveData
    public LiveData<List<Park>> getParks() { return parks; }
    public LiveData<Park> getSelectedPark() { return selectedPark; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    // Real-time listener (optional)
    private ListenerRegistration parksListener;

    // ---------------------------
    // List loading (one-shot)
    // ---------------------------
    public void loadParks() {
        loading.setValue(true);
        service.fetchParks(new FirebaseService.ParksCallback() {
            @Override public void onSuccess(List<Park> result) {
                loading.postValue(false);
                parks.postValue(result);
            }
            @Override public void onError(Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    // --------------------------------------
    // Real-time list subscription (optional)
    // --------------------------------------
    public void startObservingParks() {
        stopObservingParks();
        loading.setValue(true);
        parksListener = service.observeParks(new FirebaseService.ParksCallback() {
            @Override public void onSuccess(List<Park> result) {
                loading.postValue(false);
                parks.postValue(result);
            }
            @Override public void onError(Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void stopObservingParks() {
        if (parksListener != null) {
            parksListener.remove();
            parksListener = null;
        }
    }

    // ---------------------------
    // Detail: select a single park
    // ---------------------------
    public void selectPark(String parkId) {
        // Try to find it in the current list first (fast path)
        List<Park> current = parks.getValue();
        if (current != null) {
            for (Park p : current) {
                if (p != null && parkId.equals(p.getId())) {
                    selectedPark.setValue(p);
                    return;
                }
            }
        }
        // Fallback: fetch from Firestore
        loading.setValue(true);
        service.fetchParkById(parkId, new FirebaseService.ParkCallback() {
            @Override public void onSuccess(Park park) {
                loading.postValue(false);
                selectedPark.postValue(park);
                if (park == null) {
                    error.postValue("Park not found");
                }
            }
            @Override public void onError(Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void clearSelectedPark() {
        selectedPark.setValue(null);
    }

    // ---------------------------
    // Create / Update / Delete
    // ---------------------------
    public void addPark(Park park) {
        loading.setValue(true);
        service.addPark(park, new FirebaseService.VoidCallback() {
            @Override public void onSuccess() {
                loading.postValue(false);
                // If you use one-shot loading:
                loadParks();
                // If you use real-time, the listener will update automatically.
            }
            @Override public void onError(Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void updatePark(Park park) {
        loading.setValue(true);
        service.updatePark(park, new FirebaseService.VoidCallback() {
            @Override public void onSuccess() {
                loading.postValue(false);
                loadParks();
            }
            @Override public void onError(Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void deletePark(String parkId) {
        loading.setValue(true);
        service.deletePark(parkId, new FirebaseService.VoidCallback() {
            @Override public void onSuccess() {
                loading.postValue(false);
                loadParks();
            }
            @Override public void onError(Exception e) {
                loading.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        stopObservingParks();
        super.onCleared();
    }
}

