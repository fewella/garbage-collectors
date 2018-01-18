package Utils;

//Quazimondo fast gaussian blur:
//http://incubator.quasimondo.com/processing/gaussian_blur_1.php
public class Convolver{
    int radius;
    int kernelSize;
    int[] kernel;
    int[][] multiples;

    public Convolver(int sz){
        this.setRadius(sz);
    }

    public void setRadius(int sz){

        int i,j;
        sz=Math.min(Math.max(1,sz),248);
        if (radius==sz) return;
        kernelSize=1+sz*2;
        radius=sz;
        kernel=new int[1+sz*2];
        multiples=new int[1+sz*2][256];

        for (i=1;i<sz;i++){
            int szi=sz-i;
            kernel[sz+i]=kernel[szi]=szi;
            for (j=0;j<256;j++){
                multiples[sz+i][j]= multiples[szi][j]=kernel[szi]*j;
            }
        }
        kernel[sz]=sz;
        for (j=0;j<256;j++){
            multiples[sz][j]=kernel[sz]*j;
        }
    }
    public int[][] blur(short[][] img){

        int sum,c;
        int i,ri,xl,xi,yl,yi,ym;
        int iw=img[0].length;
        int ih=img.length;

        int img2[][]=new int[ih][iw];
        int img3[][]=new int[ih][iw];

        yi=0;

        for (yl=0;yl<ih;yl++){
            for (xl=0;xl<iw;xl++){
                c=sum=0;
                ri=xl-radius;
                for (i=0;i<kernelSize;i++){
                    xi=ri+i;
                    if (xi>=0 && xi<iw){
                        c+= multiples[i][255*img[yi][xi]];
                    }
                    sum+=kernel[i];
                }
                if(sum == 0)
                    img2[yi][xl]=0;
                else
                    img2[yi][xl]=c/sum;
            }
            yi++;
        }
        yi=0;

        for (yl=0;yl<ih;yl++){
            ym=yl-radius;
            for (xl=0;xl<iw;xl++){
                c=sum=0;
                ri=ym;
                for (i=0;i<kernelSize;i++){
                    if (ri<ih && ri>=0){
                        c+= multiples[i][img2[ri][xl]];
                    }
                    sum+=kernel[i];
                    ri++;
                }
                if(sum == 0)
                    img3[yi][xl] = 0;
                else
                    img3[yi][xl]=c/sum;
            }
            yi++;
        }
        return img3;
    }
}