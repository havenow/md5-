	void setStateOwner(uint64_t id) { m_nStateOwner = id; }
	uint64_t getStateOwner() { return m_nStateOwner; }
	uint64_t m_nStateOwner = 0;
	
int EmuManager::saveStateWithOwner(const char* fn)
{
	LOGD("saveStateWithOwner %s!\n", fn);
	fstream iofs;
	iofs.open(fn, ios::in | ios::out | ios::binary | ios::trunc);
	StateHead head;
	if (!iofs) {
		LOGE("save state fail!\n");
		return 1;
	}
	sprintf(head._sHeader, WF_STATE_HEAD);
	iofs.write(head._sHeader, 6);

	char temp[256];
	StringBuffer json;
	Document document;
	document.SetObject();
	Document::AllocatorType& allocator = document.GetAllocator();

	stringstream stream;
	string sgameid;
	stream << m_pPlayerManager->getRoom().gameid; //从long型数据输入
	stream >> sgameid; //转换为 string
	document.AddMember(KEY_START_GAMEID, sgameid.c_str(), allocator);

	string suserid;
	stream.clear();
	stream << getStateOwner(); //从long型数据输入
	stream >> suserid; //转换为 string
	document.AddMember(KEY_START_USERID, suserid.c_str(), allocator);

	rapidjson::Writer<StringBuffer> writer(json);
	document.Accept(writer);
	const char* _sJson = json.GetString();
	head._JsonLen = json.Size()+1;
	LOGD("saveStateWithOwner json size %d, %s  %llu", head._JsonLen, _sJson, m_pPlayerManager->getRoom().gameid);

	char* buf = new char[head._JsonLen + 2];
	*((uint16_t*)buf) = head._JsonLen;
	sprintf(buf + 2, "%s", _sJson);
	md5string(buf, head._JsonLen + 2, head._md5);
	iofs.write(head._md5, sizeof(head._md5));
	iofs.write(buf, head._JsonLen + 2);
	delete[] buf;

	sprintf(temp, "%stemp.ste", getEmuConfig()->getString(KEY_UGC_PATH).c_str());
	LOGD("saveStateWithOwner1 %s\n", temp);
	cocos_emu_saveState2(temp);
	LOGD("saveStateWithOwner2 %s\n", temp);
	ifstream in;
	in.open(temp, ios::binary);//打开源文件
	LOGD("saveStateWithOwner3 %s\n", temp);
	if (in.fail())//打开源文件失败
	{
		LOGE("Error 1: Fail to open the source file %s.", temp);
		in.close();
		iofs.close();
		return 1;
	}
	//复制文件
	{
		//in.seekg(0, ios::end);
		//uint32_t statesie = in.tellg();
		//LOGD("saveStateWithOwner state size %d!", statesie);
		//in.seekg(0, ios::beg);
		//iofs.write((char*)&statesie, 4);
		iofs << in.rdbuf();
		iofs.close();
		in.close();
		return 0;
	}
}

int EmuManager::loadStateWithOwner(const char* fn)
{
	ifstream in;
	in.open(fn, ios::binary);//打开源文件

	if (!in) {
		LOGE("loadStateWithOwner fail %s \n", fn);
		return 1;
	}
	StateHead head;
	in.read((char*)&head, sizeof(StateHead));
	if (in)
	{
		if (strcmp(head._sHeader, WF_STATE_HEAD) != 0)
		{
			LOGE("loadStateWithOwner old format %s!\n", head._sHeader);
			return cocos_emu_loadState2(fn);
		}
		char* sJson = new char[head._JsonLen + 2];
		*((uint16_t*)sJson) = head._JsonLen;
		in.read(sJson + 2, head._JsonLen);
		//////////////////md5验证//////////////////
		char md5[33];
		md5string(sJson, head._JsonLen + 2, md5);
		for (int i = 0; i < 32; i++)
		{
			if (md5[i] != head._md5[i])
			{
				LOGE("loadStateWithOwner md5 faild!\n");
				delete[] sJson;
				in.close();
				return 1;
			}
		}
		//////////////////read json//////////////////
		Json* root = Json_create(sJson+2);
		LOGE("loadStateWithOwner json  %s", sJson + 2);
		if (root == NULL)
		{
			LOGE("loadStateWithOwner read json error\n");
			delete[] sJson;
			in.close();
			return 1;
		}
		uint64_t gameid = atoll(Json_getString(root, KEY_START_GAMEID, "0"));
		if (m_pPlayerManager->getRoom().gameid != gameid)
		{
			LOGE("loadStateWithOwner incorrect gameid %llu\n", gameid);
			delete[] sJson;
			in.close();
			return 1;
		}
		uint64_t userid = atoll(Json_getString(root, KEY_START_USERID, "0"));
		LOGD("loadStateWithOwner owner %llu\n", userid);
		setStateOwner(userid);
		delete[] sJson;

		ofstream out;
		char tempfile[256];
		sprintf(tempfile, "%stemp.ste", getEmuConfig()->getString(KEY_UGC_PATH).c_str());
		out.open(tempfile, ios::out | ios::binary | ios::trunc);
		if (!out)
		{
			LOGE("loadStateWithOwner create temp file error %s \n", tempfile);
			out.close();
			in.close();
			return 1;
		}
		LOGD("loadStateWithOwner load temp file1 %s\n", tempfile);
		//uint32_t size = 0;
		//in.read((char*)size, 4);
		out << in.rdbuf();
		out.close();
		in.close();
		LOGD("loadStateWithOwner load temp file2 %s\n", tempfile);
		return cocos_emu_loadState2(tempfile);
	}
	in.close();
	return 1;
}