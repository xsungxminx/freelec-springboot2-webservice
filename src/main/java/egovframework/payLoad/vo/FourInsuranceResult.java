package egovframework.payLoad.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
/**
 * 4대보험 결과 DTO
 */

@Setter
@Getter
public class FourInsuranceResult {

    private BigDecimal monthlySalary;         // 보수월액

    private BigDecimal pensionEmp;            // 국민연금(근로자)
    private BigDecimal pensionEr;             // 국민연금(사업주)

    private BigDecimal healthEmp;             // 건강보험(근로자)
    private BigDecimal healthEr;              // 건강보험(사업주)

    private BigDecimal ltcEmp;                // 장기요양(근로자)
    private BigDecimal ltcEr;                 // 장기요양(사업주)

    private BigDecimal employmentEmp;         // 고용보험(근로자)
    private BigDecimal employmentEr;          // 고용보험(사업주)

    private BigDecimal industrialEr;          // 산재보험(사업주)

    private BigDecimal totalEmp;              // 근로자 4대보험 합계
    private BigDecimal totalEr;               // 사업주 4대보험 합계

    private BigDecimal incomeTax; 			  // 소득세(근로소득세)
    private BigDecimal localIncomeTax; 		  // 주민세(지방소득세)

    private BigDecimal totalDeduction; 		  // (근로자4대보험 + 소득세 + 주민세) 합계
    private BigDecimal netPay;         		  // 실지급액 = 보수월액 - totalDeduction

    // 선택: 고용보험 사업주 추가분(고용안정·직능개발) 분리 표시하고 싶다면
    private BigDecimal employmentBizEr; // 사업주만(고용안정/직능개발)

}