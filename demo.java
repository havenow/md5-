    void setStateOwner(String id) {
        m_nStateOwner = id;
    }

    String getStateOwner() {
        return m_nStateOwner;
    }

    String m_nStateOwner = "";
	
	
 /**
     * 注释：short到字节数组的转换！
     *
     * @param number
     * @return
     */
    public static byte[] shortToByte(short number) {
        int temp = number;
        byte[] b = new byte[2];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();//
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

    void saveStateWithOwner(String fn) {
        String statePath = String.format("%s%s/%s.temp.ste", AppConfig.APP_PATH, AppConfig.getNameNoExt(romPath), AppConfig.getNameNoExt(romPath));
        this.n.a.saveState(statePath);
        //fn = String.format("%s%s/%s.test", AppConfig.APP_PATH, AppConfig.getNameNoExt(romPath), AppConfig.getNameNoExt(romPath));

        File file = new File(statePath);
        if (file.exists()) {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(statePath);
                out = new FileOutputStream(fn);

                ByteBuffer byteBuffer = ByteBuffer.allocate(6);
                byte b1 = 87;
                byte b2 = 70;
                byte b3 = 83;
                byte b4 = 84;
                byte b5 = 69;
                byte b6 = 0;
                byteBuffer.put(b1);
                byteBuffer.put(b2);
                byteBuffer.put(b3);
                byteBuffer.put(b4);
                byteBuffer.put(b5);
                byteBuffer.put(b6);
                byte[] headDefalut = byteBuffer.array();

                out.write(headDefalut, 0, 6);

                MyJson json = null;
                try {
                    json = new MyJson("{\"userID\":\"000000000\",\"gameID\":\"111111111\"}");
                    if (json != null) {
                        json.put(AppConfig.KEY_START_GAMEID, GameID);
                        json.put(AppConfig.KEY_START_USERID, getStateOwner());
                    }

                    short jsonLen = (short) (json.toString().length() + 1);
                    byte[] jsonLenByte = shortToByte(jsonLen);


                    ByteBuffer buf = ByteBuffer.allocate(jsonLen + 2);
                    buf.put(jsonLenByte);
                    buf.put(json.toString().getBytes());

                    String md5 = byteToMD5(buf.array());
                    byte[] md5Byte = md5.getBytes();
                    int md5len = md5Byte.length;
                    out.write(md5Byte);
                    byte blank = 0;
                    out.write(blank);

                    out.write(jsonLenByte);
                    out.write(json.toString().getBytes());
                    out.write(blank);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);

            } catch (Exception e) {
                return;
            } finally {
                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 注释：字节数组到short的转换！
     *
     * @param b
     * @return
     */
    public short byteToShort(byte[] b) {
        short s = 0;
        short s0 = (short) (b[0] & 0xff);// 最低位
        short s1 = (short) (b[1] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }

    void loadStateWithOwner(String fn) {
        File file = new File(fn);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] bytehead = new byte[6];
            try {
                inputStream.read(bytehead);
                ByteBuffer byteBuffer = ByteBuffer.allocate(6);
                byte b1 = 87;
                byte b2 = 70;
                byte b3 = 83;
                byte b4 = 84;
                byte b5 = 69;
                byte b6 = 0;
                byteBuffer.put(b1);
                byteBuffer.put(b2);
                byteBuffer.put(b3);
                byteBuffer.put(b4);
                byteBuffer.put(b5);
                byteBuffer.put(b6);
                byte[] headDefalut = byteBuffer.array();
                if (Arrays.equals(headDefalut, bytehead)) {
                    byte[] byteMD5 = new byte[33];
                    inputStream.read(byteMD5);
                    String byteMd5String = new String(byteMD5);
                    byte[] byteJsonLen = new byte[2];
                    inputStream.read(byteJsonLen);
                    short jsonlentxString = byteToShort(byteJsonLen);
                    byte[] bytesJson = new byte[jsonlentxString];
                    inputStream.read(bytesJson);
                    byte b = 0;
                    String bytesJsonString = new String(bytesJson);
                    ByteBuffer bufferMd5 = ByteBuffer.allocate(byteJsonLen.length + bytesJson.length);
                    bufferMd5.put(byteJsonLen);
                    bufferMd5.put(bytesJson);
                    String md5 = byteToMD5(bufferMd5.array());
                    String md5S = byteMd5String.substring(0, 32);
                    if (md5.equals(md5S)) {
                        MyJson json = null;
                        try {
                            json = new MyJson(new String(bytesJson));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (json != null) {
                            String gameid = json.getString(AppConfig.KEY_START_GAMEID, "0");
                            String userid = json.getString(AppConfig.KEY_START_USERID, "0");
                            setStateOwner(userid);
                        }

                        fn = String.format("%s%s/%s.crack", AppConfig.APP_PATH, AppConfig.getNameNoExt(romPath), AppConfig.getNameNoExt(romPath));

                        OutputStream out = null;

                        try {
                            out = new FileOutputStream(fn);

                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = inputStream.read(buf)) > 0)
                                out.write(buf, 0, len);

                        } catch (Exception e) {
                            return;
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                    this.n.a.loadState(fn);
                                }
                                if (inputStream != null)
                                    inputStream.close();
                            } catch (IOException e) {
                            }
                        }

                    } else {
                        //showMessage("存档不合法");
                    }
                } else {
                    //whiteNewRecoder(dataBean, inputStream, bytehead, file, recoderCheckInterface);
                }
            } catch (IOException e) {
                e.printStackTrace();
                // showMessage("写入信息异常");
            }
        } catch (FileNotFoundException e) {

            //showMessage("文件没找到");
            e.printStackTrace();
        }
    }
	
   public static String byteToMD5(byte[] bytes) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10)
                hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();
    }
	
-----------------------------------------------------------------------------------------------------------------
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MyJson extends JSONObject {
	public MyJson() {
		// TODO Auto-generated constructor stub
		super();
	}
	public MyJson(String json) throws JSONException
	{
		super(json);
	}
	public JSONObject getJSONObject(String key , JSONObject defData){
		// TODO Auto-generated method stub
		if(null == key || key.equals(""))
			return defData;
		try {
			JSONObject vau =  getJSONObject(key);
			return vau;
		} catch (Exception e) {
			return defData;
		}
	}

public JSONArray getJSONArray(String key,JSONArray defData)  {
	if(null == key || key.equals(""))
		return defData;
	try {
		JSONArray vau =  getJSONArray(key);
		return vau;
	} catch (Exception e) {
		return defData;
	}
}
	public String getString(String key , String defData) {
		if(null == key || key.equals(""))
			return defData;
		try {
			String vau =  getString(key);
			return vau;
		} catch (Exception e) {
			return defData;
		}
	
	}
	
	public int getInt(String key , int defData) {
		if(null == key || key.equals(""))
			return defData;
		try {
			int vau =  getInt(key);
			return vau;
		} catch (Exception e) {
			return defData;
		}
	
	}
	
	public Boolean getBoolean(String key , Boolean defData) {
		if(null == key || key.equals(""))
			return defData;
		try {
			Boolean vau =  getBoolean(key);
			return vau;
		} catch (Exception e) {
			return defData;
		}
	
	}
	
	
	public Double getString(String key , Double defData) {
		if(null == key || key.equals(""))
			return defData;
		try {
			Double vau =  getDouble(key);
			return vau;
		} catch (Exception e) {
			return defData;
		}
	
	}
	
	public Long getLong(String key , Long defData) {
		if(null == key || key.equals(""))
			return defData;
		try {
			Long vau =  getLong(key);
			return vau;
		} catch (Exception e) {
			return defData;
		}
	
	}
	
	
	

}
