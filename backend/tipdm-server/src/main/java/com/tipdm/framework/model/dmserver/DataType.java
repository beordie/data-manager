package com.tipdm.framework.model.dmserver;

import com.tipdm.framework.common.utils.DateKit;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Date;

/**
 * Created by TipDM on 2016/12/21.
 * E-mail:devp@tipdm.com
 */
public enum DataType {

    numeric,
    text,
    date,
    timestamp;

    private DataType(){

    }

    public static DataType getDataType(String value){
        try {
            Date date = DateKit.convertToDate(value);
            if(date != null){
                if(value.length() > 10){
                    return DataType.timestamp;
                } else {
                    return DataType.date;
                }
            } else if(NumberUtils.isNumber(value)){
                return DataType.numeric;
            } else {
                return DataType.text;
            }
        }catch (Exception ex){
            return DataType.text;
        }
    }
}
