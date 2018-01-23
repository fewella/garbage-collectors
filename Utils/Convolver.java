package Utils;

//Quazimondo fast gaussian blur:
//http://incubator.quasimondo.com/processing/gaussian_blur_1.php
public class Convolver{
    private int sum;
    private int radius;
    private int kernelSize;
    private int[] kernel;
    private int[][] multiples;

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
        sum = 0;

        for (i=1;i<sz;i++){
            int szi=sz-i;
            kernel[sz+i]=kernel[szi]=szi;
            sum += 2*szi;
            for (j=0;j<256;j++){
                multiples[sz+i][j]= multiples[szi][j]=kernel[szi]*j;
            }
        }
        kernel[sz]=sz;
        sum += sz;
        for (j=0;j<256;j++){
            multiples[sz][j]=kernel[sz]*j;
        }
    }
    public int[][] blur(int[][] img){
        int c;
        int i,ri,xl,xi,yl,yi,ym;
        int iw=img[0].length;
        int ih=img.length;

        int img2[][]=new int[ih][iw];
        int img3[][]=new int[ih][iw];

        yi=0;

        //normalize - calculate max
        int max = 0;
        for(int[] row : img)
            for(int v : row)
                if (v > max)
                    max = v;
        if(max == 0)
            max = 1;    //prevent div by 0

        for (yl=0;yl<ih;yl++){
            for (xl=0;xl<iw;xl++){
                c=0;
                ri=xl-radius;
                for (i=0;i<kernelSize;i++){
                    xi=ri+i;
                    if (xi>=0 && xi<iw){
                        c+= multiples[i][255*img[yi][xi]/max];
                    }
                }
                img2[yi][xl]=c/sum;
            }
            yi++;
        }
        yi=0;

        for (yl=0;yl<ih;yl++){
            ym=yl-radius;
            for (xl=0;xl<iw;xl++){
                c=0;
                ri=ym;
                for (i=0;i<kernelSize;i++){
                    if (ri<ih && ri>=0){
                        c+= multiples[i][img2[ri][xl]];
                    }
                    ri++;
                }
                img3[yi][xl]=(max*c)/(255*sum);
            }
            yi++;
        }
        return img3;
    }
}