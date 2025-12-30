package egovframework.payLoad.service;


import egovframework.payLoad.mapper.InsuranceRateBandMapper;
import egovframework.payLoad.vo.FourInsuranceResult;
import egovframework.payLoad.vo.InsuranceRateBand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Resource;
import javax.validation.ValidationException;

@RequiredArgsConstructor
@Service("fourInsuranceCalcService")
public class FourInsuranceCalcService {


     private InsuranceRateBandMapper insuranceRateBandMapper;

    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal TWO    = new BigDecimal("2");

    /**
     * 연봉 기준으로 4대보험 계산(구간/요율 DB 기반)
     * @param annualSalary 연봉
     * @param companyId    회사ID
     * @param stdDate      기준일(해당 월 급여일 기준)
     */
    public FourInsuranceResult calculateByAnnual(BigDecimal annualSalary, Integer companyId, LocalDate stdDate) {
        // 1. 보수월액 계산
        BigDecimal monthlySalary = annualSalary.divide(TWELVE, 0, RoundingMode.HALF_UP);

        return calculateByMonthly(monthlySalary, companyId, stdDate);
    }

    /**
     * 월급(보수월액) 기준 4대보험 계산
     */
    public FourInsuranceResult calculateByMonthly(BigDecimal monthlySalary, Integer companyId, LocalDate stdDate) {

        FourInsuranceResult r = new FourInsuranceResult();
        r.setMonthlySalary(monthlySalary);

        // 1) 국민연금
        InsuranceRateBand pensionBand = insuranceRateBandMapper.selectApplicableBand("PENSION", null, monthlySalary, stdDate);

        if (pensionBand == null) {
            throw new IllegalStateException("국민연금 요율 설정이 없습니다.");
        }

        BigDecimal pensionBase = clamp(
                monthlySalary,
                nvl(pensionBand.getBaseMin(), monthlySalary),
                nvl(pensionBand.getBaseMax(), monthlySalary)
        );

        BigDecimal pensionEmp = round10(pensionBase.multiply(pensionBand.getEmpRate()));
        BigDecimal pensionEr  = round10(pensionBase.multiply(pensionBand.getErRate()));

        r.setPensionEmp(pensionEmp); // 근로자 요율 (근로자 부담)
        r.setPensionEr(pensionEr);	 // 사업주 요율 (사업주 부담)

        // 2) 건강보험 (구간별 요율 + 상한)
        InsuranceRateBand healthBand = insuranceRateBandMapper.selectApplicableBand("HEALTH", null, monthlySalary, stdDate);

        if (healthBand == null) {
            throw new IllegalStateException("건강보험 요율 설정이 없습니다.");
        }

        BigDecimal healthTotal = monthlySalary.multiply(healthBand.getEmpRate().add(healthBand.getErRate()));

        // 상한 적용
        if (healthBand.getPremiumMax() != null && healthTotal.compareTo(healthBand.getPremiumMax()) > 0) {
            healthTotal = healthBand.getPremiumMax();
        }

        BigDecimal healthEmp = round10(healthTotal.divide(TWO, 0, RoundingMode.HALF_UP));
        BigDecimal healthEr  = round10(healthTotal.subtract(healthEmp));

        r.setHealthEmp(healthEmp);  // 건강보험(근로자)
        r.setHealthEr(healthEr);    // 건강보험(사업주)

        // 3) 장기요양 = 건강보험 총액 × LTC 요율
        InsuranceRateBand ltcBand = insuranceRateBandMapper.selectApplicableBand("LTC", null, monthlySalary, stdDate);

        if (ltcBand == null || ltcBand.getLtcRateOfHealth() == null) {
            throw new IllegalStateException("장기요양 요율 설정이 없습니다.");
        }

        BigDecimal ltcTotal = healthTotal.multiply(ltcBand.getLtcRateOfHealth());

        BigDecimal ltcEmp = round10(ltcTotal.divide(TWO, 0, RoundingMode.HALF_UP));
        BigDecimal ltcEr  = round10(ltcTotal.subtract(ltcEmp));

        r.setLtcEmp(ltcEmp);   // 장기요양(근로자)
        r.setLtcEr(ltcEr);     // 장기요양(사업주)

        // 4) 고용보험 (회사별, 구간별 가능)
        InsuranceRateBand empBand = insuranceRateBandMapper.selectApplicableBand("EMPLOYMENT", companyId, monthlySalary,	stdDate);

        if (empBand == null) {
            throw new IllegalStateException("고용보험 요율 설정이 없습니다. companyId=" + companyId);
        }

        BigDecimal employmentEmp = round10(monthlySalary.multiply(empBand.getEmpRate()));
        BigDecimal employmentEr  = round10(monthlySalary.multiply(empBand.getErRate()));

        r.setEmploymentEmp(employmentEmp);  // 고용보험(근로자)
        r.setEmploymentEr(employmentEr);    // 고용보험(사업주)

        // 5) 산재보험 (전액 사업주, 회사별)
        InsuranceRateBand indBand = insuranceRateBandMapper.selectApplicableBand("INDUSTRIAL", companyId, monthlySalary, stdDate);

        if (indBand == null) {
            throw new IllegalStateException("산재보험 요율 설정이 없습니다. companyId=" + companyId);
        }

        BigDecimal industrialEr = round10(monthlySalary.multiply(indBand.getErRate()));
        r.setIndustrialEr(industrialEr);  // 산재보험(사업주)

        // 6) 합계
        BigDecimal totalEmp = pensionEmp // 국민연금 + 건강보험 + 장기요양 + 고용보험  (근로자)
                .add(healthEmp)
                .add(ltcEmp)
                .add(employmentEmp);

        BigDecimal totalEr = pensionEr   // 국민연금 + 건강보험 + 장기요양 + 고용보험  (사업자)
                .add(healthEr)
                .add(ltcEr)
                .add(employmentEr)
                .add(industrialEr);

        r.setTotalEmp(totalEmp);  // 근로자 4대보험 합계
        r.setTotalEr(totalEr);    // 사업주 4대보험 합계

        // (옵션) 고용보험 사업주 추가분(근로복지공단: 고용안정/직능개발 등)
        // a_insurance_rate_band에 INSURANCE_TYPE="EMPLOYMENT_BIZ"를 등록해두면 반영

        BigDecimal employmentBizEr = BigDecimal.ZERO;
        InsuranceRateBand empBizBand = insuranceRateBandMapper.selectApplicableBand( "EMPLOYMENT_BIZ", companyId, monthlySalary, stdDate);

        if(empBizBand != null && empBizBand.getErRate() != null) {
            employmentBizEr = round10(monthlySalary.multiply(empBizBand.getErRate()));
            r.setEmploymentBizEr(employmentBizEr);
            totalEr = totalEr.add(employmentBizEr); //사업주 합계에 포함
        }

        // 7) 과세표준(단순화): 보수월액 - 근로자4대보험(연금/건강/장기요양/고용)
        BigDecimal taxableBase  = monthlySalary
                .subtract(pensionEmp)
                .subtract(healthEmp)
                .subtract(ltcEmp)
                .subtract(employmentEmp);
        if(taxableBase .compareTo(BigDecimal.ZERO)  <0 ) taxableBase   = BigDecimal.ZERO;


        // 8) 소득세(구간/세율/공제 DB 기반)
        InsuranceRateBand taxBand =  insuranceRateBandMapper.selectApplicableBand("INCOME_TAX", companyId, taxableBase, stdDate);

        if (taxBand == null || taxBand.getEmpRate() == null) {
            throw new IllegalStateException("소득세(INCOME_TAX) 요율 설정이 없습니다.");
        }


        BigDecimal incomeTax = taxableBase.multiply(taxBand.getEmpRate());
        if (taxBand.getTaxDeduction() != null) {
            incomeTax = incomeTax.subtract(taxBand.getTaxDeduction());
        }
        if (incomeTax.compareTo(BigDecimal.ZERO) < 0) incomeTax = BigDecimal.ZERO;


        // 지방세
        incomeTax = round10(incomeTax);
        r.setIncomeTax(incomeTax); // ✅ 지금 VO 필드명이 incomTax라면 여기에 맞춰야 함

        BigDecimal localRate = new BigDecimal("0.10");

        // DB로 관리하려면(옵션)
        if (taxBand.getLocalTaxRate() != null) {
            localRate = taxBand.getLocalTaxRate();
        }

        BigDecimal localIncomeTax = round10(incomeTax.multiply(localRate));
        r.setLocalIncomeTax(localIncomeTax);


        BigDecimal totalDeduction = r.getTotalEmp().add(incomeTax).add(localIncomeTax);
        BigDecimal netPay = monthlySalary.subtract(totalDeduction);

        r.setTotalDeduction(totalDeduction);
        r.setNetPay(netPay);

        // 8) 소득세(구간/세율/공제 DB 기반)
        /*
         * IncomeTaxBand taxBand = incomeTaxBandMapper.selectApplicableBand(companyId,
         * taxableBase, stdDate); if (taxBand == null) { throw new
         * IllegalStateException("소득세 구간/요율 설정이 없습니다. taxableBase=" + taxableBase); }
         */

        /*
         * BigDecimal incomeTax = taxableBase.multiply(taxBand.getTaxRate()); if
         * (taxBand.getTaxDeduction() != null) { incomeTax =
         * incomeTax.subtract(taxBand.getTaxDeduction()); } if
         * (incomeTax.compareTo(BigDecimal.ZERO) < 0) incomeTax = BigDecimal.ZERO;
         */

        // 10원 미만 절사(기존 정책 유지)
        /*
         * incomeTax = round10(incomeTax); r.setIncomeTax(incomeTax);
         *
         * // 9) 주민세(지방소득세) = 소득세 * 10% (기본) BigDecimal localIncomeTax =
         * round10(incomeTax.multiply(LOCAL_TAX_RATE));
         * r.setLocalIncomeTax(localIncomeTax);
         *
         * // 10) 공제합계/실지급액 BigDecimal totalDeduction =
         * totalEmp.add(incomeTax).add(localIncomeTax); BigDecimal netPay =
         * monthlySalary.subtract(totalDeduction);
         *
         * r.setTotalDeduction(totalDeduction); r.setNetPay(netPay);
         */

        // 기존 totalEmp/totalEr는 이미 세팅되어 있으니 유지
        return r;
    }

    // ===== 유틸 메서드 =====

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    private BigDecimal nvl(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 10원 미만 절사
     */
    private BigDecimal round10(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.setScale(-1, RoundingMode.DOWN);
    }

    /**
     * 4대보험 리스트 조회
     * @param insuranceType
     * @param companyId
     * @param useYn
     * @return
     */
    public List<InsuranceRateBand> fourInstrancelist(String insuranceType, Integer companyId, String useYn) {
        return insuranceRateBandMapper.fourInstrancelist(insuranceType, companyId, useYn);
    }

    public InsuranceRateBand get(Long id) {
        return insuranceRateBandMapper.selectById(id);
    }

    /**
     * 4대보험 신규등록 / 수정
     * @param band
     */
    @Transactional
    public void fourInstranceSave(InsuranceRateBand band) {

        // 기본검증
        validateBand(band);

        // 신규/수정 저장
        if (band.getId() == null) {
            if (band.getUseYn() == null || band.getUseYn().isEmpty()) {
                band.setUseYn("Y");
            }
            insuranceRateBandMapper.insert(band);
        } else { // 수정
            insuranceRateBandMapper.update(band);
        }
    }


    @Transactional
    public void softDelete(Long id) {
        insuranceRateBandMapper.softDelete(id);
    }


    // ================== 검증 ==================

    private void validateBand(InsuranceRateBand band) {

        // 필수값 체크
        if (band.getInsuranceType() == null || band.getInsuranceType().isEmpty()) {
            throw new ValidationException("보험구부은 필수입니다.");
        }
        if (band.getWageMin() == null) {
            throw new ValidationException("보수월액 하한은 필수입니다.");
        }
        if (band.getApplyFrom() == null) {
            throw new ValidationException("적용 시작일은 필수입니다.");
        }
        if (band.getUseYn() == null || band.getUseYn().isEmpty()) {
            band.setUseYn("Y");
        }

        BigDecimal wageMin = band.getWageMin();
        BigDecimal wageMax = band.getWageMax();
        LocalDate applyFrom = band.getApplyFrom();
        LocalDate applyTo = band.getApplyTo();

        // (2) 범위 체크
        if (wageMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("보수월액 하한은 0 이상이어야 합니다.");
        }
        if (wageMax != null && wageMin.compareTo(wageMax) > 0) {
            throw new ValidationException("보수월액 하한이 상한보다 클 수 없습니다.");
        }

        if (applyTo != null && applyFrom.isAfter(applyTo)) {
            throw new ValidationException("적용 시작일이 종료일보다 클 수 없습니다.");
        }

        // 요율 기본 검증 (보험별로 다르게 가져가도 됨)
        if (!"LTC".equals(band.getInsuranceType())) {
            // 장기요양 외에는 empRate/erRate가 없는 경우 경고
            if (band.getEmpRate() == null && band.getErRate() == null) {
                throw new ValidationException("해당 보험의 요율(근로자/사업주) 중 하나 이상은 입력해야 합니다.");
            }
        } else {
            // LTC 인 경우 ltcRateOfHealth 필수
            if (band.getLtcRateOfHealth() == null) {
                throw new ValidationException("장기요양보험은 건강보험 대비 요율이 필수입니다.");
            }
        }

        // (3) 중복 구간 체크
        List<InsuranceRateBand> overlapped = insuranceRateBandMapper.selectOverlappingBands(
                band.getId(),
                band.getInsuranceType(),
                band.getCompanyId(),
                wageMin,
                wageMax,
                applyFrom,
                applyTo
        );

        if (!overlapped.isEmpty()) {
            throw new ValidationException("같은 보험구분/회사에 중복되는 보수월액·적용기간 구간이 존재합니다.");
        }


    }


}
