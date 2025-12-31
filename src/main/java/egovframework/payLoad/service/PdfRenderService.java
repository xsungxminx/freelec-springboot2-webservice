package egovframework.payLoad.service;


import java.io.*;
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
            /*String realPath = servletContext.getRealPath("/font/NanumGothic-Regular.ttf");
            if (realPath == null) {
                throw new IllegalStateException("realPath가 null 입니다. (embedded tomcat + war unpack 설정 확인 필요)");
            }

            File fontFile = new File(realPath);
            if (!fontFile.exists()) {
                throw new IllegalStateException("폰트 파일이 없습니다: " + fontFile.getAbsolutePath());
            }

            builder.useFont(fontFile, "Nanum Gothic"); // CSS랑 이름 맞추기
            URL base = servletContext.getResource("/"); */

            // ✅ classpath 폰트를 임시파일로 꺼내서 등록 (File 기반 useFont 그대로 사용)
            File fontFile = extractToTempFile("font/NanumGothic-Regular.ttf", "NanumGothic-Regular", ".ttf");
            builder.useFont(fontFile, "Nanum Gothic");

            // baseUri (상대경로 리소스가 없으면 크게 중요하지 않음)
            String baseUri = "";
            try {
                URL base = servletContext.getResource("/");
                if (base != null) baseUri = base.toExternalForm();
            } catch (Exception ignore) {
            }

            //builder.withHtmlContent(html, base.toExternalForm());
            builder.withHtmlContent(html, baseUri);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();


        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    private File extractToTempFile(String classpath, String prefix, String suffix) throws Exception {
        ClassPathResource r = new ClassPathResource(classpath);

        if (!r.exists()) {
            throw new IllegalStateException("classpath 리소스(폰트) 없음:" + classpath);
        }

        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();

        try (InputStream is = r.getInputStream();
             FileOutputStream fos = new FileOutputStream(temp)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
        }

        return temp;
    }

}
