package com.tipdm.framework.dmserver.mse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tipdm.framework.common.utils.StringKit;
import org.apache.commons.lang3.ArrayUtils;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Field;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by TipDM on 2018/1/4.
 * E-mail:devp@tipdm.com
 * 模型工具类
 */
public class ModelUtil {

    static
    public List<Field> decodeValues(JSONArray array){
        if(array == null){
            return null;
        }
        List<Field> fieldList = new ArrayList<>();
        for(int i=0; i< array.size(); i++){
            LinkedHashMap<String, Object> item = JSON.parseObject(array.getJSONObject(i).toJSONString(), LinkedHashMap.class);
            Field field = mapToField(item);
            fieldList.add(field);
        }
        return fieldList;
    }

    static
    public List<Field> filter(List<Field> fields, String[] find){
        return fields.stream().filter(x -> ArrayUtils.contains(find, x.getName())).collect(Collectors.toList());
    }

    static
    private Field mapToField(Map<String, Object> map) throws ClassCastException{

        try {
            Field field = new Field();
            String value = (String)map.get("values");
            String dataType = (String) map.get("dataType");
            DataType newType = null;
            switch (dataType) {
                case "character varying":
                    newType = DataType.STRING;
                    if(StringKit.isNotBlank(value)) {
                        field.setValues(Arrays.asList(StringKit.split(value, ",")));
                    }
                    break;
                case "numeric":{
                    newType = DataType.DOUBLE;
                    List<String> values = new ArrayList<>();
                    if(StringKit.isNotBlank(value)) {
                        double[] arr = Arrays.stream(StringKit.split(value, ","))
                                .map(String::trim).mapToDouble(Double::parseDouble).toArray();
                        double min = Arrays.stream(arr).min().getAsDouble();
                        double max = Arrays.stream(arr).max().getAsDouble();
                        values.add(min + "");
                        values.add(max + "");
                    }
                    field.setValues(values);
                    break;
                }
                case "bigint": {
                    newType = DataType.INTEGER;
                    List<String> values = new ArrayList<>();
                    if(StringKit.isNotBlank(value)) {
                        double[] arr = Arrays.stream(StringKit.split(value, ","))
                                .map(String::trim).mapToDouble(Double::parseDouble).toArray();
                        double min = Arrays.stream(arr).min().getAsDouble();
                        double max = Arrays.stream(arr).max().getAsDouble();
                        values.add(min + "");
                        values.add(max + "");
                    }
                    field.setValues(values);
                    break;
                }
                case "integer": {
                    newType = DataType.INTEGER;
                    List<String> values = new ArrayList<>();
                    if(StringKit.isNotBlank(value)) {
                        int[] arr = Arrays.stream(StringKit.split(value, ","))
                                .map(String::trim).mapToInt(Integer::parseInt).toArray();
                        int min = Arrays.stream(arr).min().getAsInt();
                        int max = Arrays.stream(arr).max().getAsInt();
                        values.add(min + "");
                        values.add(max + "");
                    }
                    field.setValues(values);
                    break;
                }
                case "date":
                    newType = DataType.DATE;
                    break;
                case "timestamp":
                    newType = DataType.DATE_TIME_SECONDS_SINCE_1970;
                    break;
                default:
                    newType = DataType.STRING;
                    if(StringKit.isNotBlank(value)) {
                        field.setValues(Arrays.asList(StringKit.split(value, ",")));
                    }
                    break;
            }
            field.setName((String) map.get("name"));
            field.setDataType(newType);
            return field;
        }catch (Exception ex){
            throw new ClassCastException("can not parse java.util.Map to convert org.openscoring.common.Field");
        }
    }
}
