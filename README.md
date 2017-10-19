md5实现文件的加密校验

- # 写文件
```c++
unsigned char* cocos_emu_saveStateBuff(int* len);

				int len;
				auto buff_state = cocos_emu_saveStateBuff(&len);

				ofstream ofs(strStatePath, ios::binary);
				if (!ofs) {
					printf("save_fail");
					return;
				}
				char state_head[6] = NET_STATE_HEAD;
				ofs.write(state_head, 6);
				unsigned char key[16];
				md5(buff_state, len, key);
				ofs.write((const char*)key, 16);
				char head_len = 4;
				ofs.put(head_len);
				int head_temp = 0;
				ofs.write((const char*)&head_temp, sizeof(head_temp));
				ofs.write((const char*)buff_state, len);
				ofs.close();
```

- # 读文件
```c++
		bool load = false;
		if (CGlobal::Instance()->GetCurChoosedNetStateIndex() != -1)
		{
			char strStatePath[512] = "";
			sprintf_s(strStatePath, "%s%s%s.%d", (EmuManager::Instance()->getEmuConfig()->getString(STATE_PATH)).c_str(),
				(EmuManager::Instance()->getEmuConfig()->getString(ROM_NAME)).c_str(), NET_STATE_EXT, CGlobal::Instance()->GetCurChoosedNetStateIndex());
			ifstream ifs(strStatePath, ios::binary);
			if (ifs)
			{
				ifs.seekg(0, ios::end);
				int len = ifs.tellg();
				ifs.seekg(0, ios::beg);
				char state_head[6];
				ifs.read(state_head, 6);
				if (strcmp(state_head, NET_STATE_HEAD) == 0)
				{
					char md_5[16];
					ifs.read(md_5, 16);

					char head_len = ifs.get();
					char* head = new char[head_len];
					ifs.read(head, head_len);

					int buf_size = len - 6 - 1 - head_len - 16;
					char* buff_state = new char[buf_size];
					ifs.read(buff_state, buf_size);
					unsigned char key[16];
					md5((unsigned char*)buff_state, buf_size, key);
					bool suc = true;
					for (int i = 0; i < 16; i++)
					{
						if (key[i] != (unsigned char)md_5[i])
						{
							suc = false;
							break;
						}
					}
					if (suc)
					{
						int ret = cocos_emu_loadStateBuff((unsigned char*)buff_state, buf_size);
						if (ret != 0)
						{
							printf("handleBattleStart load net state fail %s\n", strStatePath);
						}
						else
							load = true;

					}
					else {
						printf("net state md5 failed!\n");
					}

					delete[]buff_state;
					delete[]head;
				}
				else
				{
					printf("illegal net state!\n");
				}
				ifs.close();
			}
			else {
				printf("net state open failed!\n");
			}

		}
```
