<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.LinkedHashMap" %>
<%
    var data = new LinkedHashMap<String, Object>();
    data.put("status", "ok");
    data.put("gsonPresent", true);
    String json = new Gson().toJson(data);
    response.setContentType("application/json");
    out.print(json);
%>
