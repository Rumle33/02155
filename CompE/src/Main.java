import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Main {

    static int pc;
    static int reg[] = new int[32];
    static byte memory[];

    public static void main(String[] args) {
        String binaryFile = "src/addlarge.bin";

        try {
            memory = loadProgram(binaryFile);

            System.out.println("RISC-V Simulation Start");
            pc = 0;

            while (true) {
                if (pc >= memory.length * 4) {
                    System.out.println("Program counter out of memory bounds.");
                    break;
                }

                int instr = memory[pc >> 2];
                int opcode = instr & 0x7f;
                int rd = (instr >> 7) & 0b11111;
                int funct3 = (instr >> 12) & 0x07;
                int rs1 = (instr >> 15) & 0b11111;
                int rs2 = (instr >> 20) & 0b11111;

                int imm = instr >> 20; // Extract immediate
                if ((imm & 0x800) != 0) {
                    imm = imm | 0xFFFFF000;
                }

                switch (opcode) {
                    case 0x13: // Immediate operations: ADDI, XORI, ORI, ANDI
                        switch (funct3) {
                            case 0x0: // ADDI
                                reg[rd] = reg[rs1] + imm;
                                break;
                            case 0x4: // XORI
                                reg[rd] = reg[rs1] ^ imm;
                                break;
                            case 0x6: // ORI
                                reg[rd] = reg[rs1] | imm;
                                break;
                            case 0x7: // ANDI
                                reg[rd] = reg[rs1] & imm;
                                break;
                            default:
                                System.out.println("Immediate operation funct3 " + funct3 + " not implemented");
                        }
                        break;

                    case 0x33: // operations: ADD, SUB, XOR, OR, AND
                        int funct7 = (instr >> 25) & 0x7F; // Extract funct7
                        switch (funct3) {
                            case 0x0: // ADD or SUB
                                if (funct7 == 0x00) { // ADD
                                    reg[rd] = reg[rs1] + reg[rs2];
                                } else if (funct7 == 0x20) { // SUB
                                    reg[rd] = reg[rs1] - reg[rs2];
                                } else {
                                    System.out.println("Register operation funct7 " + funct7 + " not implemented");
                                }
                                break;
                            case 0x4: // XOR
                                reg[rd] = reg[rs1] ^ reg[rs2];
                                break;
                            case 0x6: // OR
                                reg[rd] = reg[rs1] | reg[rs2];
                                break;
                            case 0x7: // AND
                                System.out.printf("ANDI: rs1=%d (0x%x), imm=%d (0x%x)\n", reg[rs1], reg[rs1], imm, imm);
                                reg[rd] = reg[rs1] & imm;
                                break;
                            default:
                                System.out.println("Register operation funct3 " + funct3 + " not implemented");
                        }
                        break;

                    case 0x03: // Load instructions
                    switch (funct3) {
                        case 0x2: // LW (Load Word)
                            int loadAddress = reg[rs1] + imm; // Calculate address
                            if (loadAddress < 0 || loadAddress + 4 > memory.length * 4) {
                                System.out.println("Memory load address out of bounds: " + loadAddress);
                            } else {
                                reg[rd] = ByteBuffer.wrap(memory, loadAddress, 4)
                                                    .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                                                    .getInt();
                            }
                            break;
                        default:
                            System.out.println("Load funct3 " + funct3 + " not implemented");
                    }
                    break;
            
                case 0x23: // Store instructions
                    switch (funct3) {
                        case 0x2: // SW (Store Word)
                            int storeAddress = reg[rs1] + imm; // Calculate address
                            if (storeAddress < 0 || storeAddress + 4 > memory.length * 4) {
                                System.out.println("Memory store address out of bounds: " + storeAddress);
                            } else {
                                ByteBuffer.wrap(memory, storeAddress, 4)
                                            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                                            .putInt(reg[rs2]);
                            }
                            break;
                        default:
                            System.out.println("Store funct3 " + funct3 + " not implemented");
                    }
                    break;



                    case 0x37: // LUI
                        reg[rd] = (instr & 0xFFFFF000);
                        break;

                    case 0x17: // AUIPC
                        reg[rd] = pc + (instr & 0xFFFFF000);
                        break;

                    case 0x73: // ECALL
                        if (reg[17] == 10) {
                            System.out.println("ECALL: Program Terminated");
                            return;
                        } else {
                            System.out.println("Unhandled ECALL");
                        }
                        break;
                    

                    default:
                        System.out.println("Opcode " + opcode + " not yet implemented");
                }


                pc += 4; // move to neext instruc


                for (int i = 0; i < reg.length; ++i) {
                    System.out.print("x" + i + "=" + reg[i] + " ");
                }
                System.out.println();
            }

            System.out.println("RISC-V Simulation End");
            saveRegisterDump();

        } catch (IOException e) {
            System.out.println("Error loading program: " + e.getMessage());
        }
    }

    //Function to load a program bin files
    static byte[] loadProgram(String binaryFile) throws IOException {
        FileInputStream fis = new FileInputStream(binaryFile);
        byte[] buffer = fis.readAllBytes(); // Read entire file as bytes
        fis.close();
    
        // Ensure the memory is large enough to hold the program
        byte[] memory = new byte[1024 * 1024]; // Allocate 1 MB of memory
        System.arraycopy(buffer, 0, memory, 0, buffer.length); // Copy the program to the start of memory
    
        return memory;
    }
    
    // makes the regdump files
    static void saveRegisterDump() throws IOException {
        String outputFile = "register_dump.bin";
        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
        for (int regVal : reg) {
            fos.write(ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(regVal).array());
        }
        fos.close();
        System.out.println("Register dump saved to " + outputFile);
    }

}
