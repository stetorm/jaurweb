package com.steto.jaurinv;



import com.steto.jaurlib.response.AI_ModelsEnum;
import com.steto.jaurlib.response.AI_NationEnum;
import com.steto.jaurlib.response.AI_TransformerType;
import com.steto.jaurlib.response.AI_Type;

/**
 * Created by stefano on 29/11/14.
 */
public class AuroraVersionData {
    public AI_ModelsEnum modelName;
    public AI_NationEnum nation;
    public AI_TransformerType transformerType;
    public AI_Type type;

    public AuroraVersionData(AI_ModelsEnum modelName, AI_NationEnum nation, AI_TransformerType transformerType, AI_Type type) {
        this.modelName = modelName;
        this.nation = nation;
        this.transformerType = transformerType;
        this.type = type;
    }
}
