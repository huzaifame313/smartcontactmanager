package com.smart.controller;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;
    //method for adding common data to response
    @ModelAttribute
    public void addCommonData(Model model,Principal principal){
        String userName= principal.getName();
        System.out.println("userName"+userName);
        User user=userRepository.getUserByUserName(userName);
        System.out.println("User"+user);
        model.addAttribute("user",user);
    }
    //dashboard home
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal){
        model.addAttribute("title","User Dashboard");
        return "normal/user_dashboard";
    }
    //open add form handler
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model){
        model.addAttribute("title","add contact");
        model.addAttribute("contact",new Contact());
        return "normal/add_contact_form";
    }
//   process add contact form
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact,
                                 @RequestParam("profileImage") MultipartFile file,
                                 Principal principal, HttpSession session){
        try {
            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);
//        processing and uploading file
            if (file.isEmpty()){
//                if the file is empty try your message
                System.out.println("File is empty");
                contact.setImage("contactl.png");
            }else {
//                file the file to folder update the name to contact
                contact.setImage(file.getOriginalFilename());
                File saveFile= new ClassPathResource("static/image").getFile();
                Path path=Paths.get(saveFile.getAbsolutePath()+File.pathSeparator+file.getOriginalFilename());
                Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Image is uploaded");
            }
            contact.setUser(user);

            user.getContacts().add(contact);

            this.userRepository.save(user);

            System.out.println("Data" + contact);
            System.out.println("Added to database");
            // message success
            session.setAttribute("message",new Message("Your Contact is added!! Add more","success"));
        }catch (Exception e){
            System.out.println("ERROR"+e.getMessage());
            e.printStackTrace();
            //error message
            session.setAttribute("message",new Message("Something went Wrong!!Try again","danger"));
        }
        return "normal/add_contact_form";
    }
    //show contact handler
    //per page= 5[n]
    //current page=0[page]
    @GetMapping("/show-contacts/{page}")
    public String showContact(@PathVariable("page") Integer page, Model model,Principal principal){
        model.addAttribute("title","Show user Contacts ");
        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);
        //currentPage=page
        //contact per page-5
        Pageable pageable = PageRequest.of(page, 5);
        Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
        model.addAttribute("contacts",contacts);
        model.addAttribute("currentPage",page);
        model.addAttribute("totalPages",contacts.getTotalPages());
//send list of contact
        return "normal/show_contacts";
    }
    //show particular contact details
    @RequestMapping("/{cId}/contact")
    public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal ){
        Optional<Contact> contactOptional = this.contactRepository.findById(cId);
        Contact contact = contactOptional.get();
        String userName= principal.getName();
        User user = this.userRepository.getUserByUserName(userName);
       if (user.getId()==contact.getUser().getId()){
           model.addAttribute("contact",contact);
           model.addAttribute("title",contact.getName());
       }
        return "normal/contact_detail";
    }
    @GetMapping("/delete/{cid}")
    @Transactional
    public String deleteContact(@PathVariable ("cid") Integer cId,Model model,HttpSession session){
        Optional<Contact> optionalContact = this.contactRepository.findById(cId);
        Contact contact = optionalContact.get();
        //check....
        //remoce
        //image
        //contect.getImage()
        this.contactRepository.delete(contact);


        session.setAttribute("message",new Message("Contact delete successfully","success"));
        return "redirect:/user/show-contacts/0";
    }
    //open update form handler
    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid,  Model model){
        model.addAttribute("title","update contact");
        Contact contact = this.contactRepository.findById(cid).get();
        model.addAttribute("contact",contact);
        return "normal/update_form";
    }
    //update contact handler
    @RequestMapping(value = "process-update",method = RequestMethod.POST)
    public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model model,HttpSession session,Principal principal){
       try {
           // image
           //old contact details
           Contact oldContactDetails = this.contactRepository.findById(contact.getCId()).get();
           if (!file.isEmpty()){
              //file work
               //rewrite
//               delete old photo
               File deleteFile= new ClassPathResource("static/image").getFile();
               File file1=new File(deleteFile,oldContactDetails.getImage());
               file1.delete();

               //update photo
               contact.setImage(file.getOriginalFilename());
               File saveFile= new ClassPathResource("static/image").getFile();
               Path path=Paths.get(saveFile.getAbsolutePath()+File.pathSeparator+file.getOriginalFilename());
               Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
               contact.setImage(file.getOriginalFilename());
           }else {
               contact.setImage(oldContactDetails.getImage());
           }
           User user = this.userRepository.getUserByUserName(principal.getName());
           contact.setUser(user);
           this.contactRepository.save(contact);
           session.setAttribute("message",new Message("Your Contact is Updated","success"));
       }catch (Exception e){
           e.printStackTrace();
       }
        System.out.println("contact name"+contact.getName());
        System.out.println("contact id"+contact.getCId());
        return "redirect:/user/"+contact.getCId()+"/contact";
    }
    //profile handler
    @GetMapping("/profile")
    public String yourProfile(Model model){
        model.addAttribute("title","Profile Page");
        return "normal/profile";
    }
}
