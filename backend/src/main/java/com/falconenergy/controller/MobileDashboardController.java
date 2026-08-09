package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.DriverProfileResponse;
import com.falconenergy.dto.MobileDashboardResponse;
import com.falconenergy.dto.NotificationResponse;
import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.DeliveryRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.MobileDashboardService;
import com.falconenergy.service.ProofOfDeliveryStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/mobile", "/api/mobile"})
public class MobileDashboardController {

    private final MobileDashboardService mobileDashboardService;
    private final ProofOfDeliveryStorageService proofOfDeliveryStorageService;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;

    public MobileDashboardController(MobileDashboardService mobileDashboardService,
                                     ProofOfDeliveryStorageService proofOfDeliveryStorageService,
                                     DeliveryRepository deliveryRepository,
                                     UserRepository userRepository) {
        this.mobileDashboardService = mobileDashboardService;
        this.proofOfDeliveryStorageService = proofOfDeliveryStorageService;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<MobileDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver mobile dashboard retrieved successfully", mobileDashboardService.getDashboard()));
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<MobileDashboardResponse.RecentDelivery>>> getDeliveries() {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver mobile deliveries list retrieved successfully", mobileDashboardService.getDeliveries()));
    }

    @GetMapping("/deliveries/{deliveryId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<MobileDashboardResponse.RecentDelivery>> getDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver mobile delivery retrieved successfully", mobileDashboardService.getDelivery(deliveryId)));
    }

    @PostMapping("/deliveries/{deliveryId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> acceptDelivery(@PathVariable Long deliveryId) {
        mobileDashboardService.acceptDelivery(deliveryId);
        return ResponseEntity.ok(ApiResponse.success("Delivery accepted successfully"));
    }

    public static class StartTripRequest {
        public Double latitude;
        public Double longitude;
    }

    @PostMapping("/deliveries/{deliveryId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> startTrip(@PathVariable Long deliveryId, @RequestBody StartTripRequest request) {
        mobileDashboardService.startTrip(deliveryId, request.latitude, request.longitude);
        return ResponseEntity.ok(ApiResponse.success("Trip started successfully"));
    }

    public static class ArriveRequest {
        public String receivedBy;
        public String remarks;
    }

    @PostMapping("/deliveries/{deliveryId}/arrive")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> arriveAtDestination(@PathVariable Long deliveryId, @RequestBody ArriveRequest request) {
        mobileDashboardService.arriveAtDestination(deliveryId, request.receivedBy, request.remarks);
        return ResponseEntity.ok(ApiResponse.success("Arrived at destination recorded"));
    }

    @PostMapping(value = "/deliveries/{deliveryId}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> uploadProof(
            @PathVariable Long deliveryId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "notes", required = false) String notes) {
        mobileDashboardService.uploadProof(deliveryId, file, latitude, longitude, notes);
        return ResponseEntity.ok(ApiResponse.success("Proof of delivery uploaded successfully"));
    }

    @PostMapping("/deliveries/{deliveryId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> completeDelivery(@PathVariable Long deliveryId) {
        mobileDashboardService.completeDelivery(deliveryId);
        return ResponseEntity.ok(ApiResponse.success("Delivery completed successfully"));
    }

    @GetMapping("/deliveries/{deliveryId}/proof")
    public ResponseEntity<byte[]> getProofFile(@PathVariable Long deliveryId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found with id: " + deliveryId));

        if (delivery.getPodPhotoPath() == null || delivery.getPodPhotoPath().isEmpty()) {
            throw new ResourceNotFoundException("Proof of delivery photo is not uploaded yet.");
        }

        // Secure Authorization check:
        // User must either be an admin/manager/operator or be the driver assigned to this delivery.
        boolean isAuthorizedRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                               a.getAuthority().equals("ROLE_MANAGER") || 
                               a.getAuthority().equals("ROLE_OPERATIONS") ||
                               a.getAuthority().equals("ROLE_OPERATOR"));

        if (!isAuthorizedRole) {
            User user = userRepository.findByEmail(auth.getName())
                    .or(() -> userRepository.findByUsername(auth.getName()))
                    .orElseThrow(() -> new AccessDeniedException("User not found"));

            if (user.getRole() != UserRole.DRIVER || user.getDriver() == null ||
                delivery.getLoadingActivity() == null || 
                delivery.getLoadingActivity().getVehicle() == null || 
                delivery.getLoadingActivity().getVehicle().getDriver() == null ||
                !delivery.getLoadingActivity().getVehicle().getDriver().getId().equals(user.getDriver().getId())) {
                throw new AccessDeniedException("Access denied. You do not own this delivery.");
            }
        }

        byte[] fileBytes = proofOfDeliveryStorageService.loadFile(delivery.getPodPhotoPath());
        
        String ext = delivery.getPodPhotoPath().substring(delivery.getPodPhotoPath().lastIndexOf(".") + 1).toLowerCase();
        String contentType = "image/jpeg";
        if ("png".equals(ext)) {
            contentType = "image/png";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileBytes);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver profile retrieved successfully", mobileDashboardService.getProfile()));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications() {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver notifications retrieved successfully", mobileDashboardService.getNotifications()));
    }

    @PostMapping("/notifications/{id}/read")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Void>> readNotification(@PathVariable Long id) {
        mobileDashboardService.markNotificationAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }
}
