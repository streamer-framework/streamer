package cea;

import cea.util.GlobalUtils;

public class ModelToRedis {

	public static void main(String[] args) {
		String path="/home/sandra/AIConfiance/F18_2022/code/domain-shift/models/bestmodel.h5";
		String modelName="autoencodermodel";
		//String path= new GlobalUtils().getAbsoluteBaseProjectPath() + "src/main/resources/algs/domain_shift/models/bestmodel.h5";
		
		if(args.length == 2) {
			modelName=args[0];
			path=args[1];
		}
		cea.util.GlobalUtils.fileFromDiskToRedis(modelName, path);
		System.out.println("Model stored succesfully");
	}

}
