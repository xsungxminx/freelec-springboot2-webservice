package egovframework.payLoad.mapper;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import egovframework.payLoad.vo.InsuranceRateBand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

//import org.egovframe.rte.psl.dataaccess.mapper.Mapper;

//@Mapper("insuranceRateBandMapper")
@Mapper
public interface InsuranceRateBandMapper{

    // 리스트 조회(간단 필터)
    List<InsuranceRateBand> fourInstrancelist(@Param("insuranceType") String insuranceType, @Param("companyId") Integer companyId, @Param("useYn") String useYn);

    InsuranceRateBand selectById(@Param("id") Long id);

    void insert(InsuranceRateBand band);

    void update(InsuranceRateBand band);

    void delete(@Param("id") Long id);          // 물리 삭제용(선호 안하면 안 써도 됨)

    void softDelete(@Param("id") Long id);      // use_yn = 'N'


    /**
     * 특정 보험종류, 회사, 보수월액, 기준일에 해당하는 구간(가장 우선순위 높은 1건) 조회
     */
    InsuranceRateBand selectApplicableBand(
            @Param("insuranceType") String insuranceType, // PENSION, HEALTH, LTC, EMPLOYMENT, INDUSTRIAL(4대보험 국민,건강,고용,산재)
            @Param("companyId") Integer companyId,
            @Param("wage") BigDecimal wage,
            @Param("stdDate") LocalDate stdDate
    );

    List<InsuranceRateBand> selectOverlappingBands(Long id, String insuranceType, Integer companyId, BigDecimal wageMin,
                                                   BigDecimal wageMax, LocalDate applyFrom, LocalDate applyTo);





}