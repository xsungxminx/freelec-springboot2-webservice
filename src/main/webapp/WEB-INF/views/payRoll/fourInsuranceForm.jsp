<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>급여 4대보험 계산</title>

    <style>
        body { font-family: "맑은 고딕", Arial, sans-serif; background:#f5f6fa; padding:24px; }
        .container { max-width:900px; margin:0 auto; background:#fff; border-radius:10px;
                     padding:24px 28px 32px; box-shadow:0 4px 18px rgba(0,0,0,0.06); }
        h2 { margin-top:0; font-size:22px; }
        .desc { font-size:13px; color:#777; margin-bottom:16px; }

        .form-row { margin-bottom:10px; display:flex; align-items:center; gap:10px; }
        .form-row label { width:120px; font-weight:600; }
        .form-row input { flex:1; padding:6px 8px; border:1px solid #ced4da; border-radius:6px; }

        .btn { padding:8px 16px; border-radius:6px; border:1px solid transparent;
               cursor:pointer; font-size:14px; font-weight:600; }
        .btn-primary { background:#4b7bec; color:#fff; }
        .btn-primary:hover { background:#3867d6; }

        .result-box { margin-top:24px; border-top:1px solid #e5e7f0; padding-top:18px; display:none; }
        table { width:100%; border-collapse:collapse; font-size:13px; }
        th,td { border-bottom:1px solid #edf0f5; padding:6px 8px; text-align:right; }
        th { background:#f8f9fb; text-align:center; }
        .text-left { text-align:left; }
        .summary { margin-top:10px; font-size:13px; color:#555; }
        .error { margin-top:10px; color:#c00; font-weight:600; }
    </style>

    <script>
        function formatNumber(num) {
            if (num == null) return "-";
            return Number(num).toLocaleString('ko-KR');
        }

        function onCalcClick() {
            var annualSalary = document.getElementById("annualSalary").value;
            annualSalary = (annualSalary || "").replace(/,/g,'').trim(); // ✅ 콤마 제거

            var companyId    = document.getElementById("companyId").value;
            var stdDate      = document.getElementById("stdDate").value;

            if (!annualSalary) { alert("연봉을 입력하세요."); return; }
            if (!stdDate)      { alert("기준일을 입력하세요."); return; }

            var params = "annualSalary=" + encodeURIComponent(annualSalary)
                       + "&stdDate="    + encodeURIComponent(stdDate);


            // 회사ID가 비어있으면 보내지 않음
            if (companyId && companyId.trim() !== "") {
                params += "&companyId=" + encodeURIComponent(companyId.trim());
            }

            fetch("<c:url value='/payroll/result.do'/>", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
                body: params
            })
            .then(function(res) {
                if (!res.ok) throw new Error("API 호출 실패 : " + res.status);
                return res.json();
            })
            .then(function(data) {
                document.getElementById("errMsg").textContent = "";
                document.getElementById("resultBox").style.display = "block";

                // 상단 요약
                document.getElementById("monthlySalary").textContent = formatNumber(data.monthlySalary);
                document.getElementById("totalEmp").textContent      = formatNumber(data.totalEmp);
                document.getElementById("totalEr").textContent       = formatNumber(data.totalEr);

                //document.getElementById("incomeTax").textContent      = formatNumber(data.incomeTax);
                //document.getElementById("localIncomeTax").textContent = formatNumber(data.localIncomeTax);

                document.getElementById("incomeTaxTop").textContent        = formatNumber(data.incomeTax);
				document.getElementById("localIncomeTaxTop").textContent   = formatNumber(data.localIncomeTax);

				document.getElementById("incomeTaxBottom").textContent     = formatNumber(data.incomeTax);
				document.getElementById("localIncomeTaxBottom").textContent= formatNumber(data.localIncomeTax);

                document.getElementById("totalDeduction").textContent = formatNumber(data.totalDeduction);
                document.getElementById("netPay").textContent         = formatNumber(data.netPay);

                // 상세 항목
                document.getElementById("pensionEmp").textContent     = formatNumber(data.pensionEmp);
                document.getElementById("pensionEr").textContent      = formatNumber(data.pensionEr);

                document.getElementById("healthEmp").textContent      = formatNumber(data.healthEmp);
                document.getElementById("healthEr").textContent       = formatNumber(data.healthEr);

                document.getElementById("ltcEmp").textContent         = formatNumber(data.ltcEmp);
                document.getElementById("ltcEr").textContent          = formatNumber(data.ltcEr);

                document.getElementById("empEmp").textContent         = formatNumber(data.employmentEmp);
                document.getElementById("empEr").textContent          = formatNumber(data.employmentEr);

                document.getElementById("indEr").textContent          = formatNumber(data.industrialEr);

            })
            .catch(function(err) {
                document.getElementById("errMsg").textContent = err.message;
                document.getElementById("resultBox").style.display = "none";
            });
        }

        function downloadPdf(){
        	  var annualSalary = document.getElementById("annualSalary").value;
        	  var companyId    = document.getElementById("companyId").value;
        	  var stdDate      = document.getElementById("stdDate").value;

        	  if (!annualSalary) { alert("연봉을 입력하세요."); return; }
        	  if (!stdDate) { alert("기준일을 입력하세요."); return; }

        	  var params = "annualSalary=" + encodeURIComponent(annualSalary)
        	             + "&stdDate=" + encodeURIComponent(stdDate);

        	  if (companyId && companyId.trim() !== "") {
        	    params += "&companyId=" + encodeURIComponent(companyId.trim());
        	  }

        	  // POST로 파일 다운로드는 fetch보다 form submit이 안정적
        	  var form = document.createElement("form");
        	  form.method = "POST";
        	  form.action = "<c:url value='/wonit/payroll/result.pdf'/>";
        	  form.target = "_blank";

        	  params.split("&").forEach(function(kv){
        	    var p = kv.split("=");
        	    var input = document.createElement("input");
        	    input.type = "hidden";
        	    input.name = decodeURIComponent(p[0]);
        	    input.value = decodeURIComponent(p[1] || "");
        	    form.appendChild(input);
        	  });

        	  document.body.appendChild(form);
        	  form.submit();
        	  form.remove();
        	}



    </script>
</head>
<body>
<div class="container">
    <h2>급여 4대보험 계산</h2>
    <div class="desc">
        a_insurance_rate_band에 등록된 요율·구간을 기준으로 연봉/회사/기준일에 따른 4대보험 공제금액을 계산합니다.
    </div>

    <!-- 입력영역 -->
    <div class="form-row">
        <label>연봉(원)</label>
        <input type="text" id="annualSalary" placeholder="예: 50,000,000">
    </div>
    <div class="form-row">
        <label>회사ID</label>
        <input type="text" id="companyId" placeholder="회사별 요율이 있으면 입력, 공통이면 비워두기">
    </div>
    <div class="form-row">
        <label>기준일</label>
        <input type="date" id="stdDate">
    </div>

    <div class="form-row" style="justify-content:flex-end;">
        <button type="button" class="btn btn-primary" onclick="onCalcClick()">급여계산</button>
        <button type="button" class="btn btn-primary" onclick="downloadPdf()">PDF 출력</button>
    </div>

    <div id="errMsg" class="error"></div>

    <!-- 결과 영역 -->
    <div id="resultBox" class="result-box">
        <div class="summary">
            <b>보수월액 :</b> <span id="monthlySalary"></span> 원 &nbsp;&nbsp;|&nbsp;&nbsp;
            <b>근로자 4대보험 합계 :</b> <span id="totalEmp"></span> 원 &nbsp;&nbsp;|&nbsp;&nbsp;
            <b>사업주 4대보험 합계 :</b> <span id="totalEr"></span> 원
             <br/>
			 <b>소득세 :</b> <span id="incomeTaxTop"></span> 원 &nbsp;&nbsp;|&nbsp;&nbsp;
			 <b>주민세 :</b> <span id="localIncomeTaxTop"></span> 원 &nbsp;&nbsp;|&nbsp;&nbsp;
			 <b>총 공제 :</b> <span id="totalDeduction"></span> 원 &nbsp;&nbsp;|&nbsp;&nbsp;
			 <b>실지급액 :</b> <span id="netPay"></span> 원
        </div>

        <table>
            <thead>
            <tr>
                <th class="text-left">보험종류</th>
                <th>근로자 부담</th>
                <th>사업주 부담</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="text-left">국민연금</td>
                <td id="pensionEmp"></td>
                <td id="pensionEr"></td>
            </tr>
            <tr>
                <td class="text-left">건강보험</td>
                <td id="healthEmp"></td>
                <td id="healthEr"></td>
            </tr>
            <tr>
                <td class="text-left">장기요양</td>
                <td id="ltcEmp"></td>
                <td id="ltcEr"></td>
            </tr>
            <tr>
                <td class="text-left">고용보험</td>
                <td id="empEmp"></td>
                <td id="empEr"></td>
            </tr>
            <tr>
                <td class="text-left">산재보험</td>
                <td>-</td>
                <td id="indEr"></td>
            </tr>
            <tr>
			  <td class="text-left">소득세</td>
			  <td id="incomeTaxBottom"></td>
			  <td>-</td>
			</tr>
			<tr>
			  <td class="text-left">주민세</td>
			  <td id="localIncomeTaxBottom"></td>
			  <td>-</td>
			</tr>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
