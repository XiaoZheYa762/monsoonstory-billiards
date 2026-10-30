package dev.monsoonstory.billiards.music;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Validates Ogg framing, page CRC, a single Vorbis stream, and declared duration. */
public final class OggValidator {
    public record Info(int rate,int channels,long samples){}
    private static final int[] CRC=new int[256];
    static{for(int i=0;i<256;i++){int r=i<<24;for(int j=0;j<8;j++)r=(r<<1)^((r<0)?0x04c11db7:0);CRC[i]=r;}}
    private static long little(byte[] b,int p,int n){long value=0;for(int i=0;i<n;i++)value|=(b[p+i]&255L)<<(8*i);return value;}
    private static int crc(byte[] h,byte[] l,byte[] b){int crc=0;for(byte[] part:new byte[][]{h,l,b})for(byte v:part)crc=(crc<<8)^CRC[((crc>>>24)^(v&255))&255];return crc;}
    public static Info validate(Path file,long maxBytes,int maxSeconds)throws IOException{
        long size=Files.size(file);if(size<64||size>maxBytes)throw new IOException("size");
        int serial=0,sequence=0,packets=0,rate=0,channels=0;long granule=-1,lastGranule=-1;boolean eos=false,continued=false;
        var packet=new ByteArrayOutputStream();
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
            long consumed=0;
            while(consumed<size){
                if(eos)throw new IOException("chained stream");
                byte[] header=in.readNBytes(27);if(header.length!=27||header[0]!='O'||header[1]!='g'||header[2]!='g'||header[3]!='S'||header[4]!=0)throw new IOException("Ogg header");
                int flags=header[5]&255;if((flags&~7)!=0||((flags&1)!=0)!=continued)throw new IOException("Ogg continuation");
                int pageSerial=(int)little(header,14,4),pageSequence=(int)little(header,18,4);
                if(sequence==0){serial=pageSerial;if((flags&2)==0)throw new IOException("missing BOS");}else if((flags&2)!=0)throw new IOException("duplicate BOS");
                if(pageSerial!=serial||pageSequence!=sequence++)throw new IOException("multiplexed or reordered Ogg");
                int segments=header[26]&255;byte[] lengths=in.readNBytes(segments);if(lengths.length!=segments)throw new EOFException();int bodyLength=0;for(byte l:lengths)bodyLength+=l&255;
                byte[] body=in.readNBytes(bodyLength);if(body.length!=bodyLength)throw new EOFException();consumed+=27+segments+bodyLength;
                int expected=(int)little(header,22,4);Arrays.fill(header,22,26,(byte)0);if(crc(header,lengths,body)!=expected)throw new IOException("Ogg CRC");
                granule=little(header,6,8);if(granule>=0){if(granule<lastGranule)throw new IOException("Ogg granule");lastGranule=granule;}
                int offset=0;for(byte length:lengths){int n=length&255;if(packets<3){if(packet.size()+n>1024*1024)throw new IOException("Vorbis header too large");packet.write(body,offset,n);}offset+=n;continued=n==255;if(n<255){if(packets<3){byte[] data=packet.toByteArray();packet.reset();if(data.length<7||data[0]!=(packets==0?1:packets==1?3:5)||!Arrays.equals(Arrays.copyOfRange(data,1,7),new byte[]{'v','o','r','b','i','s'}))throw new IOException("not Vorbis");if(packets==0){if(data.length!=30||little(data,7,4)!=0||data[29]!=1)throw new IOException("Vorbis identification");channels=data[11]&255;rate=(int)little(data,12,4);int a=data[28]&15,b=(data[28]>>>4)&15;if(channels<1||channels>2||rate<8000||rate>192000||a<6||b>13||a>b)throw new IOException("unsupported Vorbis");}}packets++;}}
                eos=(flags&4)!=0;if(eos&&(continued||granule<1))throw new IOException("bad EOS");
                if(rate>0&&lastGranule>rate*(long)maxSeconds)throw new IOException("duration");
            }
        }
        if(!eos||packets<4||lastGranule<1||rate==0)throw new IOException("incomplete Vorbis");
        return new Info(rate,channels,lastGranule);
    }
    private OggValidator(){}
}
