package cis3334.java_firebase_parklist.data.firebase;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import cis3334.java_firebase_parklist.data.model.Park;

public class FirebaseService {

    // ---------- Callback types ----------
    public interface ParksCallback {
        void onSuccess(List<Park> parks);
        void onError(Exception e);
    }

    public interface ParkCallback {
        void onSuccess(@Nullable Park park);
        void onError(Exception e);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // ---------- Constants ----------
    private static final String TAG = "FirebaseService";
    private static final String COLLECTION_PARKS = "parks";

    // ---------- State ----------
    private final FirebaseFirestore db;

    public FirebaseService() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ============================================================
    // One-shot: fetch all parks
    // ============================================================
    public void fetchParks(final ParksCallback callback) {
        db.collection(COLLECTION_PARKS)
                // .orderBy("name")  // optional if you have a "name" field and index
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<Park> parks = new ArrayList<>();
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                Park p = doc.toObject(Park.class);
                                if (p != null) {
                                    p.setId(doc.getId());  // keep Firestore document id
                                    parks.add(p);
                                }
                            }
                            callback.onSuccess(parks);
                        } else {
                            Exception e = (task.getException() != null)
                                    ? task.getException()
                                    : new Exception("Unknown Firestore error");
                            Log.e(TAG, "fetchParks failed", e);
                            callback.onError(e);
                        }
                    }
                });
    }

    // ============================================================
    // Real-time: observe all parks (remember to remove() the return value)
    // ============================================================
    public ListenerRegistration observeParks(final ParksCallback callback) {
        return db.collection(COLLECTION_PARKS)
                // .orderBy("name")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "observeParks listen failed", error);
                            callback.onError(error);
                            return;
                        }
                        List<Park> parks = new ArrayList<>();
                        if (value != null) {
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Park p = doc.toObject(Park.class);
                                if (p != null) {
                                    p.setId(doc.getId());
                                    parks.add(p);
                                }
                            }
                        }
                        callback.onSuccess(parks);
                    }
                });
    }

    // ============================================================
    // Fetch a single park by id
    // ============================================================
    public void fetchParkById(@NonNull String id, final ParkCallback callback) {
        db.collection(COLLECTION_PARKS).document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    Park p = doc.toObject(Park.class);
                    if (p != null) p.setId(doc.getId());
                    callback.onSuccess(p); // may be null if not found
                })
                .addOnFailureListener(callback::onError);
    }

    // ============================================================
    // Add or upsert a park
    // - if park.id is null/empty, Firestore generates the id
    // - if park.id is set, we write to that document id
    // ============================================================
    public void addPark(@NonNull Park park, final VoidCallback callback) {
        String id = park.getId();
        if (id == null || id.isEmpty()) {
            // Auto-ID
            db.collection(COLLECTION_PARKS)
                    .add(park)
                    .addOnSuccessListener((DocumentReference ref) -> callback.onSuccess())
                    .addOnFailureListener(callback::onError);
        } else {
            // Upsert with known id
            db.collection(COLLECTION_PARKS).document(id)
                    .set(park)
                    .addOnSuccessListener(v -> callback.onSuccess())
                    .addOnFailureListener(callback::onError);
        }
    }

    // ============================================================
    // Update an existing park (requires non-empty id)
    // ============================================================
    public void updatePark(@NonNull Park park, final VoidCallback callback) {
        String id = park.getId();
        if (id == null || id.isEmpty()) {
            callback.onError(new IllegalArgumentException("updatePark: id is required"));
            return;
        }
        db.collection(COLLECTION_PARKS).document(id)
                .set(park)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    // ============================================================
    // Delete by id
    // ============================================================
    public void deletePark(@NonNull String id, final VoidCallback callback) {
        db.collection(COLLECTION_PARKS).document(id)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}

