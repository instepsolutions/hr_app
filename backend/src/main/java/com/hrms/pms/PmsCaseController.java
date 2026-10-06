package com.hrms.pms;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/pms")
@Tag(name = "PMS Cases and Appraisal Summary")
public class PmsCaseController {

    private final PipManagementService pips;
    private final AppraisalSummaryService summaries;

    public PmsCaseController(PipManagementService pips, AppraisalSummaryService summaries) {
        this.pips = pips;
        this.summaries = summaries;
    }

    @GetMapping("/appraisals/summary/overview")
    public Map<String, Object> appraisalSummaryOverview(@RequestParam Map<String, String> filters) {
        return summaries.overview(filters);
    }

    @GetMapping("/appraisals/summary")
    public Object appraisalSummaryList(@RequestParam Map<String, String> filters) {
        return summaries.list(filters);
    }

    @GetMapping("/appraisals/summary/{appraisalId}")
    public Map<String, Object> appraisalSummaryDetail(@PathVariable long appraisalId) {
        return summaries.detail(appraisalId);
    }

    @GetMapping("/appraisals/summary/export")
    public ResponseEntity<byte[]> exportAppraisalSummary(@RequestParam Map<String, String> filters) {
        return PmsDashboardController.csvResponse(summaries.export(filters), "appraisal-summary-" + LocalDate.now() + ".csv");
    }

    @GetMapping("/pips/overview")
    public Map<String, Object> pipOverview(@RequestParam(required = false) Long departmentId) {
        return pips.overview(departmentId);
    }

    @GetMapping("/pips")
    public Object pipList(@RequestParam Map<String, String> filters,
                          @RequestParam(required = false) Long departmentId) {
        return pips.list(filters, departmentId);
    }

    @GetMapping("/pips/templates")
    public Object pipTemplates() {
        return pips.templates();
    }

    @GetMapping("/pips/export")
    public ResponseEntity<byte[]> exportPips(@RequestParam Map<String, String> filters,
                                             @RequestParam(required = false) Long departmentId) {
        return PmsDashboardController.csvResponse(pips.export(filters, departmentId), "pip-report-" + LocalDate.now() + ".csv");
    }

    @PostMapping("/pips")
    public Map<String, Object> createPipDraft(@RequestBody Map<String, Object> payload) {
        return pips.createDraft(payload);
    }

    @GetMapping("/pips/{pipId}")
    public Map<String, Object> pipDetail(@PathVariable long pipId) {
        return pips.detail(pipId);
    }

    @PutMapping("/pips/{pipId}")
    public Map<String, Object> updatePip(@PathVariable long pipId, @RequestBody Map<String, Object> payload) {
        return pips.update(pipId, payload);
    }

    @PostMapping("/pips/{pipId}/start")
    public Map<String, Object> startPip(@PathVariable long pipId) {
        return pips.start(pipId);
    }

    @PostMapping("/pips/{pipId}/reviews")
    public Map<String, Object> addPipReview(@PathVariable long pipId, @RequestBody Map<String, Object> payload) {
        return pips.addReview(pipId, payload);
    }

    @PostMapping("/pips/{pipId}/extend")
    public Map<String, Object> extendPip(@PathVariable long pipId, @RequestBody Map<String, Object> payload) {
        return pips.extend(pipId, payload);
    }

    @PostMapping("/pips/{pipId}/close")
    public Map<String, Object> closePip(@PathVariable long pipId, @RequestBody Map<String, Object> payload) {
        return pips.close(pipId, payload);
    }

    @PostMapping("/pips/{pipId}/cancel")
    public Map<String, Object> cancelPip(@PathVariable long pipId, @RequestBody Map<String, Object> payload) {
        return pips.cancel(pipId, Objects.toString(payload.get("reason"), ""));
    }

    @PostMapping("/pips/{pipId}/remind")
    public Map<String, Object> remindPip(@PathVariable long pipId) {
        return pips.remind(pipId);
    }
}