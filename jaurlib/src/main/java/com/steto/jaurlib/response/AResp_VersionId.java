package com.steto.jaurlib.response;

import com.steto.jaurlib.modbus.MB_code;

/**
 * Created by sbrega on 27/11/2014.
 */
public class AResp_VersionId extends AuroraResponse{




    public AResp_VersionId(MB_code code) {
        super(code);

    }

    public AResp_VersionId() {
        super();

    }


    @Override
    public String toString() {
        String localDescription = "Model: "+getModelName()+", Nationality: "+getNationality()+", Transformer "+getTransformerInfo()+", Type: "+getType();
        return description.isEmpty() ? super.toString() : description+" "+localDescription;
    }

    private String getType() {
        AI_Type type = AI_Type.fromCode(getParam4());
        return type==null ?  "UNKNOWN" : type.toString() ;
    }

    private String getTransformerInfo() {
        AI_TransformerType type = AI_TransformerType.fromCode(getParam3());
        return type==null ?  "UNKNOWN" : type.toString() ;
    }

    private String getNationality() {
        AI_NationEnum type = AI_NationEnum.fromCode(getParam2());
        return type==null ?  "UNKNOWN" : type.toString() ;
    }


    String getModelName()
    {
        AI_ModelsEnum type = AI_ModelsEnum.fromCode(getParam1());
        return type==null ?  "UNKNOWN" : type.toString() ;
    }
}
