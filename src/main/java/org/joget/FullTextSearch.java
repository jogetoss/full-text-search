package org.joget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.apps.datalist.model.DataListFilterTypeDefault;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;

public class FullTextSearch extends DataListFilterTypeDefault {
    private final static String MESSAGE_PATH = "messages/FullTextSearch";

    public String getName() {
        return "Full Text Search";
    }

    public String getVersion() {
        return "6.0.2";
    }
    
    public String getClassName() {
        return getClass().getName();
    }
    
    public String getLabel() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.FullTextSearch.pluginLabel", getClassName(), MESSAGE_PATH);
    }
    
    public String getDescription() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.FullTextSearch.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/fullTextSearch.json", null, true, MESSAGE_PATH);
    }
    
    public String getTemplate(DataList datalist, String name, String label) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map dataModel = new HashMap();
        dataModel.put("name", datalist.getDataListEncodedParamName(DataList.PARAMETER_FILTER_PREFIX+name));
        dataModel.put("label", label);
        dataModel.put("value", getValue(datalist, name, getPropertyString("defaultValue")));
        dataModel.put("contextPath", WorkflowUtil.getHttpServletRequest().getContextPath());
        return pluginManager.getPluginFreeMarkerTemplate(dataModel, getClassName(), "/templates/fullTextSearch.ftl", null);
    }

    public DataListFilterQueryObject getQueryObject(DataList datalist, String name) {
        DataListFilterQueryObject queryObject = new DataListFilterQueryObject();
        String value = getValue(datalist, name, getPropertyString("defaultValue"));
        if (datalist != null && datalist.getBinder() != null && value != null && !value.isEmpty()) {
           
            if ("true".equalsIgnoreCase(getPropertyString("useCustomQuery"))) {
                String query = getPropertyString("filterQuery");
                Collection<String> args = new ArrayList<String>();
                
                String findStr = "?";
                int lastIndex = 0;
                int count = 0;

                while(lastIndex != -1){

                    lastIndex = query.indexOf(findStr,lastIndex);

                    if(lastIndex != -1){
                        count ++;
                        lastIndex += findStr.length();
                        
                        args.add(value);
                    }
                }
                
                queryObject.setQuery(query);
                queryObject.setValues(args.toArray(new String[0]));
            } else {
                String[] columns = getPropertyString("columns").split(";");
                
                String query = "";
                Collection<String> args = new ArrayList<String>();
                
                String[] keyword;
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    keyword = new String[]{value.substring(1, value.length() - 1)};
                } else {
                    keyword = value.split(" ");
                }
                
                String searchCondition = getPropertyString("searchCondition");
                
                for (String c : columns) {
                    String columnName = datalist.getBinder().getColumnName(c);
                    
                    if (!query.isEmpty()) {
                        query += " OR ";
                    }
                    
                    query += "(";
                    
                    boolean firstKeywordEntry = true;
                    
                    for (String k : keyword) {
                        if (!k.isEmpty()) {
                            if (!firstKeywordEntry) {
                                if ("OR".equals(searchCondition)) {
                                    query += " OR ";
                                } else {
                                    query += " AND ";
                                }
                            }
                            query += "lower(" + columnName + ") like lower(?)";
                            args.add('%' + k + '%');
                            
                            firstKeywordEntry = false;
                        }
                    }
                    
                    query += ")";
                }
                
                queryObject.setQuery("(" + query + ")");
                queryObject.setValues(args.toArray(new String[0]));
            }

            return queryObject;
        }
        return null;
    }
}   
