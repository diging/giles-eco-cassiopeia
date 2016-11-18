<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="jumbotron col-md-12">

<sec:authorize access="isAnonymous()">
<h1>Cassiopeia</h1>
<p>
Harness the power of <a href="https://github.com/tesseract-ocr">Tessearct</a> through a simple REST service.
</p>
</sec:authorize>
<sec:authorize access="isAuthenticated()">
<h1>Whoohoo!</h1>
<p>You are logged in.</p>
</sec:authorize>
</div>