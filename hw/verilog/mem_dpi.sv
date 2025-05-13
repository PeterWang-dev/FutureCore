import "DPI-C" function int pmem_read(input int raddr);
import "DPI-C" function void pmem_write(
  input int  waddr,
  input int  wdata,
  input byte wmask
);

module ram_dpi (
    input wire valid,
    input wire [31:0] raddr,
    input wire wen,
    input wire [31:0] waddr,
    input wire [31:0] wdata,
    input wire [7:0] wmask,
    output reg [31:0] rdata
);
  always_ff begin
    if (valid) begin  // 有读写请求时
      rdata = pmem_read(raddr);
      if (wen) begin  // 有写请求时
        pmem_write(waddr, wdata, wmask);
      end
    end else begin
      rdata = 0;
    end
  end
endmodule

module rom_dpi (
    input wire valid,
    input wire [31:0] raddr,
    output reg [31:0] rdata
);
  always_ff begin
    if (valid) begin
      rdata = pmem_read(raddr);
    end else begin
      rdata = 0;
    end
  end
endmodule
