package egovframework.payLoad.service;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

import javax.servlet.ServletContext;

import egovframework.payLoad.vo.FourInsuranceResult;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;


import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PdfRenderService {

    private final TemplateEngine templateEngine;

    private final ServletContext servletContext;


    public byte[] renderFourInsurancePdf(FourInsuranceResult result, Integer companyId, String stdDate) {
        try {
            // 1) Thymeleaf -> HTML
            Context ctx = new Context();
            ctx.setVariable("result", result);
            ctx.setVariable("companyId", companyId);
            ctx.setVariable("stdDate", stdDate);


            // ✅ 추가(없으면 템플릿에서 '-' 처리됨)
            ctx.setVariable("payYm", stdDate.substring(0, 7).replace("-", "년 ") + "월 급여명세서");
            ctx.setVariable("payDate", stdDate);
//            ctx.setVariable("empName", result.getEmpName());   // 있다면
//            ctx.setVariable("empNo", result.getEmpNo());       // 있다면
//            ctx.setVariable("deptName", result.getDeptName()); // 있다면
//            ctx.setVariable("positionName", result.getPositionName()); // 있다면


            String html = templateEngine.process("payroll/fourInsurancePdf", ctx)
                    .replace("&nbsp;", "&#160;");

            // 2) HTML -> PDF
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // ✅ webapp/font 아래 폰트 사용
            String realPath = servletContext.getRealPath("/font/NanumGothic-Regular.ttf");
            if (realPath == null) {
                throw new IllegalStateException("realPath가 null 입니다. (embedded tomcat + war unpack 설정 확인 필요)");
            }

            File fontFile = new File(realPath);
            if (!fontFile.exists()) {
                throw new IllegalStateException("폰트 파일이 없습니다: " + fontFile.getAbsolutePath());
            }

            builder.useFont(fontFile, "Nanum Gothic"); // CSS랑 이름 맞추기

            URL base = servletContext.getResource("/");
            builder.withHtmlContent(html, base.toExternalForm());
            builder.toStream(out);
            builder.run();

            return out.toByteArray();


        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }
}
