package egovframework.payLoad.controller;


import egovframework.payLoad.service.FourInsuranceCalcService;
import egovframework.payLoad.service.PdfRenderService;
import egovframework.payLoad.vo.FourInsuranceResult;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/payroll")
public class PayrollApicontroller {

    private Log log = LogFactory.getLog(this.getClass());

    @Autowired
    private FourInsuranceCalcService fourInsuranceCalcService;

    @Autowired
    private PdfRenderService pdfRenderService;


    @PostMapping("/result.do")
    public FourInsuranceResult fourInsuranceCalc(@RequestParam("annualSalary") BigDecimal annualSalary,
                                                 @RequestParam(value = "companyId", required = false) String companyIdStr,
                                                 @RequestParam("stdDate") String stdDate, Model model) {

            log.debug("call : payroll/result.do");

            Integer companyId = null;
            if (companyIdStr != null && !companyIdStr.trim().isEmpty()){
                companyId =Integer.valueOf(companyIdStr.trim());
            }

            // 1) yyyy-MM-dd 또는 yyyyMMdd 둘 다 허용하고 싶다면:
            stdDate = stdDate.trim();

            LocalDate date;

            if(stdDate.contains("-")){
                // 2025-11-02 형식
                date = LocalDate.parse(stdDate);  // 기본 포맷이 yyyy-MM-dd
            }else {
                // 20251102 형식
                date = LocalDate.parse(stdDate, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            }

        log.debug("fourInsuranceCalc annualSalary=" + annualSalary + ", companyId=" + companyId + ", stdDate=" + date);

        return fourInsuranceCalcService.calculateByAnnual(annualSalary, companyId, date);
    }

    @PostMapping(value ="/result.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> fourInsurancePdf(@RequestParam("annualSalary") BigDecimal annualSalary, @RequestParam(value="companyId", required=false) String companyIdStr, @RequestParam("stdDate") String stdDate) {

        log.debug("log : fourInsurancePdf");

        Integer companyId = null;
        if (companyIdStr != null && !companyIdStr.trim().isEmpty()) {
            companyId = Integer.valueOf(companyIdStr.trim());
        }

        stdDate = stdDate.trim();
        LocalDate date = stdDate.contains("-")
                ? LocalDate.parse(stdDate)
                : LocalDate.parse(stdDate, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        FourInsuranceResult result = fourInsuranceCalcService.calculateByAnnual(annualSalary, companyId, date);

        byte[] pdfBytes = pdfRenderService.renderFourInsurancePdf(result, companyId, date.toString());

        String filename = "four-insurance" + date + ".pdf";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

}
