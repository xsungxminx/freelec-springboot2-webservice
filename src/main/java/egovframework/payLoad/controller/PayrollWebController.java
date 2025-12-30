package egovframework.payLoad.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.validation.ValidationException;

import egovframework.payLoad.service.FourInsuranceCalcService;
import egovframework.payLoad.vo.FourInsuranceResult;
import egovframework.payLoad.vo.InsuranceRateBand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
@RequestMapping("/payroll")
public class PayrollWebController {

    private Log log = LogFactory.getLog(this.getClass());

    @Autowired
    private FourInsuranceCalcService fourInsuranceCalcService;


    @GetMapping("/fourInsuranceForm.do")
    public String fourInsuranceForm() {

        log.debug("log : fourInsuranceForm");

        return "/payRoll/fourInsuranceForm"; // 연봉/회사id 입력 폼 JSP
    }


    @PostMapping("/fourInsuranceCalc")
    public String fourInsuranceCalc(@RequestParam("annualSalary") BigDecimal annualSalary,
                                    @RequestParam("companyId") Integer companyId,
                                    @RequestParam("stdDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate stdDate, Model model) {

        log.debug("log : fourInsuranceCalc");


        FourInsuranceResult result = fourInsuranceCalcService.calculateByAnnual(annualSalary, companyId, stdDate);

        model.addAttribute("result", result);
        model.addAttribute("annualSalary", annualSalary);
        model.addAttribute("companyId", companyId);
        model.addAttribute("stdDate", stdDate);

        return "payroll/fourInsuranceResult"; // 결과 보여주는 JSP
    }



    /**
     * 리스트 화면
     * 예) GET /admin/insRateBand/list?insuranceType=HEALTH&companyId=1
     */
    @GetMapping("/fourInstrancelist.do")
    public String fourInstrancelist(@RequestParam(value = "insuranceType", required = false) String insuranceType,
                                    @RequestParam(value = "companyId", required = false) Integer companyId,
                                    @RequestParam(value = "useYn", required = false, defaultValue = "Y") String useYn, Model model) {


        log.debug("fourInstrancelist");

        List<InsuranceRateBand> list = fourInsuranceCalcService.fourInstrancelist(insuranceType, companyId, useYn);

        model.addAttribute("list", list);
        model.addAttribute("insuranceType", insuranceType);
        model.addAttribute("companyId", companyId);
        model.addAttribute("useYn", useYn);

        //return "admin/insRateBand/list"; // /WEB-INF/views/admin/insRateBand/list.jsp

        return "/wonit/sale/payRoll/fourInstrancelist"; // 연봉/회사id 입력 폼 JSP
    }

    /**
     * 4대보험 신규등록 form(화면)
     * @param id
     * @param model
     * @return
     */
    @GetMapping("/registFourInstrance.do")
    public String registFourInstrance(@RequestParam(value = "id", required = false) Long id, Model model) {

        log.debug("registFourInstrance");


        InsuranceRateBand band;

        if (id != null) {
            band = fourInsuranceCalcService.get(id);
        } else {
            band = new InsuranceRateBand();
            band.setUseYn("Y");
        }

        model.addAttribute("band", band);

        // 보험구분 select박스용 (하드코딩 or 공통코드)
        //model.addAttribute("insuranceTypes", new String[] { "PENSION", "HEALTH", "LTC", "EMPLOYMENT", "INDUSTRIAL" });
        model.addAttribute("insuranceTypes", new String[] { "PENSION", "HEALTH", "LTC", "EMPLOYMENT", "INDUSTRIAL" });

        return "/wonit/sale/payRoll/registFourInstrance";
    }

    /**
     * 저장 처리 (insert / update)
     */
    @PostMapping("/fourInstranceSave.do")
    public String fourInstranceSave(InsuranceRateBand band, Model model) {

        log.debug("fourInstranceSave");

        try {
            fourInsuranceCalcService.fourInstranceSave(band);
            return "redirect:/wonit/payroll/fourInstrancelist.do?insuranceType=" + band.getInsuranceType();

        } catch (ValidationException e) {
            // 검증 실패시 다시 폼으로
            model.addAttribute("band", band);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("insuranceTypes",
                    new String[]{"PENSION", "HEALTH", "LTC", "EMPLOYMENT", "INDUSTRIAL"});

            return "/wonit/sale/payRoll/registFourInstrance";
        }

    }


    /**
     * 소프트 삭제
     */
    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id, @RequestParam("insuranceType") String insuranceType) {

        log.debug("delete");

        fourInsuranceCalcService.softDelete(id);
        return "redirect:/wonit/payroll/fourInstrancelist?insuranceType=" + insuranceType;
    }


}
