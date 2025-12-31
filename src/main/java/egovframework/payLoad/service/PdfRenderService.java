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

    private volatile File cachedNanumFont;


    public byte[] renderFourInsurancePdf(FourInsuranceResult result, Integer companyId, String stdDate) {

        long t0 = System.currentTimeMillis();

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
            //File fontFile = extractToTempFile("font/NanumGothic-Regular.ttf", "NanumGothic-Regular", ".ttf");
            //builder.useFont(fontFile, "Nanum Gothic");

            /*builder.useFont(
                    () -> {
                        try {
                            return new ClassPathResource("font/NanumGothic-Regular.ttf").getInputStream();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    "Nanum Gothic"
            );*/


            // baseUri (상대경로 리소스가 없으면 크게 중요하지 않음)
            /*String baseUri = "";
            try {
                URL base = servletContext.getResource("/");
                if (base != null) baseUri = base.toExternalForm();
            } catch (Exception ignore) {
            }*/

            builder.useFastMode(); // ✅ 속도 개선
            builder.useFont(getNanumFontOnce(), "Nanum Gothic");

            // 외부 리소스가 없으면 baseUri 없어도 됨(about:blank로)
            builder.withHtmlContent(html, "about:blank");



            //builder.withHtmlContent(html, base.toExternalForm());
            //builder.withHtmlContent(html, baseUri);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();


        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }  finally {
            System.out.println("[PDF] elapsed(ms)=" + (System.currentTimeMillis() - t0));
        }
    }


    private File getNanumFontOnce() throws Exception {
        File f = cachedNanumFont;
        if (f != null && f.exists()) return f;

        synchronized (this) {
            if (cachedNanumFont != null && cachedNanumFont.exists()) return cachedNanumFont;
            cachedNanumFont = extractToTempFile("font/NanumGothic-Regular.ttf", "NanumGothic", ".ttf");
            // deleteOnExit()는 제거 권장 (서버 재기동 시 새로 생성되므로)
            return cachedNanumFont;
        }
    }


    private File extractToTempFile(String classpath, String prefix, String suffix) throws Exception {
        ClassPathResource r = new ClassPathResource(classpath);
        if (!r.exists()) throw new IllegalStateException("classpath 폰트 없음: " + classpath);

        File temp = File.createTempFile(prefix, suffix);
        try (InputStream is = r.getInputStream();
             FileOutputStream fos = new FileOutputStream(temp)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        }
        return temp;
    }

}
