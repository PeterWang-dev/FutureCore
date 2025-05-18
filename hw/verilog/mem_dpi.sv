import "DPI-C" context function int pmem_read(input int raddr);
import "DPI-C" context function void pmem_write(
  input int  waddr,
  input int  wdata,
  input byte wmask
);

module ram_dpi (
    input wire clk,
    input wire resetn,
    input wire valid,
    input wire [31:0] raddr,
    input wire wen,
    input wire [31:0] waddr,
    input wire [31:0] wdata,
    input wire [7:0] wmask,
    output logic [31:0] rdata
);

  always_ff @(posedge clk) begin
    if (resetn & valid) begin
      if (wen) begin  // 有写请求时
        pmem_write(waddr, wdata, wmask);
      end
    end
  end

  assign rdata = resetn & valid ? pmem_read(raddr) : 0;

endmodule

module rom_dpi (
    input wire clk,
    input wire resetn,
    input wire valid,
    input wire [31:0] raddr,
    output logic [31:0] rdata
);

  assign rdata = resetn & valid ? pmem_read(raddr) : 0;

endmodule
