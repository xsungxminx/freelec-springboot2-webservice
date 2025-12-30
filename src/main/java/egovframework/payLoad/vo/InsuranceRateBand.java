package egovframework.payLoad.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;
/**
 * 4대보험 구간별 요율/상한·하한 테이블 DTO
 */
@Setter
@Getter
public class InsuranceRateBand {

    private Long id;
    private String insuranceType;       // PENSION, HEALTH, LTC, EMPLOYMENT, INDUSTRIAL(4대보험 국민,건강,고용,산재)
    private Integer companyId;

    private BigDecimal wageMin;        // 보수월액 하한(이상)
    private BigDecimal wageMax;		   // 보수월액 상한(이하, NULL이면 무한대)

    private BigDecimal empRate;		   // 근로자 요율(예: 0.045 = 4.5%)
    private BigDecimal erRate;		   // 사업주 요율

    private BigDecimal baseMin;		   // 기준소득월액 하한(국민연금용)
    private BigDecimal baseMax;		   // 기준소득월액 상한(국민연금용)

    private BigDecimal premiumMax;	   // 총 보험료 상한(건강보험 등)

    private BigDecimal ltcRateOfHealth; // 장기요양: 건강보험료 대비 요율(예: 0.1295)

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyFrom;        // 적용 시작일

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyTo;		    // 적용 종료일

    private BigDecimal taxDeduction;    // 누진공제(원)
    private BigDecimal localTaxRate;    // 주민세율(예: 0.10)

    private String useYn;

    // getter / setter ...
}
