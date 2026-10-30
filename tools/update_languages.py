"""Maintain matching Chinese and English keys for public additions."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'common/src/main/resources/assets/monsoonstory_billiards'
MUSIC={
 'manage':('服务器音乐','Server music'), 'title':('球桌音乐','Table music'),
 'upload':('上传 OGG','Upload OGG'), 'refresh':('刷新','Refresh'),
 'select':('用于此桌','Assign to table'), 'play':('播放','Play'),
 'pause':('暂停','Pause'), 'stop':('停止','Stop'),
 'loop_on':('循环：开','Loop: on'), 'loop_off':('循环：关','Loop: off'),
 'delete':('删除曲目','Delete track'), 'delete_confirm':('确认删除？','Confirm delete?'),
 'status_preparing':('正在校验音乐','Validating music'),
 'status_uploading':('正在上传','Uploading'), 'status_uploaded':('上传完成','Upload complete'),
 'status_downloading':('正在下载','Downloading'), 'status_ready':('音乐已缓存','Music cached'),
 'status_choosing':('请选择 OGG 文件','Choose an OGG file'),
 'status_permission':('仅管理员可管理音乐','Only operators can manage music'),
 'status_distance':('请回到球桌附近','Move closer to the table'),
 'status_limit':('文件超限或上传繁忙','File limit or upload busy'),
 'status_quota':('服务器曲库容量已满','Server library is full'),
 'status_storage':('音乐文件读写失败','Music storage error'),
 'status_format':('需要有效且未超限的 OGG Vorbis','Valid OGG Vorbis within limits required'),
 'status_invalid':('音乐请求无效','Invalid music request'),
 'status_missing':('请先选择可用曲目','Select an available track'),
 'status_download':('音乐下载已中断','Music download interrupted'),
 'status_busy':('音乐处理繁忙','Music processing busy'),
 'status_cancelled':('音乐操作已取消','Music operation cancelled'),
 'status_timeout':('音乐传输超时','Music transfer timed out'),
}
for index,locale in enumerate(('zh_cn','en_us')):
    file=ASSETS/'lang'/(locale+'.json')
    values=json.loads(file.read_text(encoding='utf-8'))
    values['itemGroup.monsoonstory_billiards']=('季风物语：桌球','Monsoon Story: Billiards')[index]
    for key,value in MUSIC.items():values['ui.monsoonstory_billiards.music_'+key]=value[index]
    if locale=='en_us':
        for key,value in values.items():
            if isinstance(value,str):values[key]=value.replace('Jifeng Billiards','Monsoon Story: Billiards').replace('Jifeng Six-pocket','Monsoon Story Six-pocket')
    file.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
sounds=ASSETS/'sounds.json'
values=json.loads(sounds.read_text(encoding='utf-8'))
values['user_music']={'sounds':[{'name':'monsoonstory_billiards:user_music','stream':True}]}
sounds.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Updated public translations and user-music stream entry.')
