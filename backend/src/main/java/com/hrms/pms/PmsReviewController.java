package com.hrms.pms;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pms/reviews")
@Tag(name = "PMS Reviews")
public class PmsReviewController {

    private final PmsReviewService reviews;

    public PmsReviewController(PmsReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/manager/overview")
    @Operation(summary = "Manager review overview and assigned-team analytics")
    public Map<String, Object> managerOverview(@RequestParam(required = false) Long cycleId,
                                               @RequestParam(required = false) Long departmentId) {
        return reviews.managerOverview(cycleId, departmentId);
    }

    @GetMapping("/manager")
    public List<Map<String, Object>> managerQueue(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Long departmentId,
                                                  @RequestParam(required = false) Integer rating,
                                                  @RequestParam(required = false) Long cycleId) {
        return reviews.managerQueue(search, status, departmentId, rating, cycleId);
    }

    @GetMapping("/manager/{appraisalId}")
    public Map<String, Object> managerReview(@PathVariable long appraisalId) {
        return reviews.managerReview(appraisalId);
    }

    @PutMapping("/manager/{appraisalId}/draft")
    public Map<String, Object> saveManagerDraft(@PathVariable long appraisalId,
                                               @RequestBody Map<String, Object> payload) {
        return reviews.saveManagerDraft(appraisalId, payload);
    }

    @PostMapping("/manager/{appraisalId}/submit")
    public Map<String, Object> submitManagerReview(@PathVariable long appraisalId) {
        return reviews.submitManagerReview(appraisalId);
    }

    @PostMapping("/manager/{appraisalId}/send-back")
    public Map<String, Object> sendBackToEmployee(@PathVariable long appraisalId,
                                                 @RequestBody Map<String, Object> payload) {
        return reviews.sendBackToEmployee(appraisalId, payload);
    }

    @PostMapping("/manager/{appraisalId}/clarification")
    public Map<String, Object> requestClarification(@PathVariable long appraisalId,
                                                    @RequestBody Map<String, Object> payload) {
        return reviews.requestClarification(appraisalId, payload);
    }

    @PostMapping("/manager/{appraisalId}/remind")
    public Map<String, Object> remindManager(@PathVariable long appraisalId) {
        return reviews.remindManager(appraisalId);
    }

    @GetMapping("/hr/overview")
    @Operation(summary = "HR review overview and assigned workload analytics")
    public Map<String, Object> hrOverview(@RequestParam(required = false) Long cycleId,
                                          @RequestParam(required = false) Long departmentId) {
        return reviews.hrOverview(cycleId, departmentId);
    }

    @GetMapping("/hr")
    public List<Map<String, Object>> hrQueue(@RequestParam(required = false) String search,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long departmentId,
                                             @RequestParam(required = false) Long managerId,
                                             @RequestParam(required = false) Long reviewerId,
                                             @RequestParam(required = false) Integer rating,
                                             @RequestParam(required = false) Long cycleId) {
        return reviews.hrQueue(search, status, departmentId, managerId, reviewerId, rating, cycleId);
    }

    @GetMapping("/hr/reviewers")
    public List<Map<String, Object>> hrReviewers() {
        return reviews.hrReviewers();
    }

    @GetMapping("/hr/{appraisalId}")
    public Map<String, Object> hrReview(@PathVariable long appraisalId) {
        return reviews.hrReview(appraisalId);
    }

    @PutMapping("/hr/{appraisalId}/draft")
    public Map<String, Object> saveHrDraft(@PathVariable long appraisalId,
                                         @RequestBody Map<String, Object> payload) {
        return reviews.saveHrDraft(appraisalId, payload);
    }

    @PostMapping("/hr/{appraisalId}/submit")
    public Map<String, Object> submitHrReview(@PathVariable long appraisalId) {
        return reviews.submitHrReview(appraisalId);
    }

    @PostMapping("/hr/{appraisalId}/reassign")
    public Map<String, Object> reassignHrReview(@PathVariable long appraisalId,
                                               @RequestBody Map<String, Object> payload) {
        return reviews.reassignHrReview(appraisalId, payload);
    }

    @PostMapping("/hr/{appraisalId}/remind")
    public Map<String, Object> remindHrReviewer(@PathVariable long appraisalId) {
        return reviews.remindHrReviewer(appraisalId);
    }

    @GetMapping("/{appraisalId}/history")
    public List<Map<String, Object>> reviewHistory(@PathVariable long appraisalId) {
        return reviews.reviewHistory(appraisalId);
    }

    @GetMapping("/{reviewType}/export")
    @Operation(summary = "Download manager or HR review data as CSV")
    public ResponseEntity<byte[]> export(@PathVariable String reviewType,
                                         @RequestParam(required = false) Long cycleId,
                                         @RequestParam(required = false) Long departmentId) {
        String type = reviewType.toUpperCase();
        String filename = type.equals("MANAGER") ? "manager-review-" : "hr-review-";
        return PmsDashboardController.csvResponse(reviews.exportCsv(type, cycleId, departmentId),
                filename + LocalDate.now() + ".csv");
    }
}