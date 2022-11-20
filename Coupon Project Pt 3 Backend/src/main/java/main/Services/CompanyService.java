package main.Services;

import main.Beans.Category;
import main.Beans.Company;
import main.Beans.Coupon;
import main.Exceptions.AlreadyExistsException;
import main.Exceptions.CouponExpiredException;
import main.Exceptions.NotFoundException;
import main.Exceptions.IncorrectCredentialsException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Scope("prototype")
public class CompanyService extends ClientService{
    private int companyId;

    public CompanyService() {
    }

    @Override
    public int login(String email, String password){
        Optional<Company> company = companyRepo.findByEmailAndPassword(email, password);
        if (company.isPresent()) {
            companyId = company.get().getId();
            return companyId;
        }
        else return -1;
    }

    //CREATE

    /** checks if the coupon's companyId matches that of the company that adds it, then checks that the label
     * doesn't match that of one of the company's other coupons, and then adds the coupon to the database.
     * @param coupon the coupon to be added
     * @throws AlreadyExistsException coupon already exists
     * @throws NotFoundException company ID doesn't match
     */
    public Coupon addCoupon(Coupon coupon) throws NotFoundException, AlreadyExistsException, CouponExpiredException {
        coupon.setCompanyId(companyId);
        List<Coupon> companyCoupons = couponRepo.findAllByCompanyId(companyId);
        //find if coupon already exists

        for (Coupon compCoup : companyCoupons) {
            if (compCoup.getTitle().equals(coupon.getTitle()))
                throw new AlreadyExistsException("Coupon already exists");
        }
        //find if endDate has passed
        if(coupon.getEndDate().toLocalDate().isBefore(LocalDate.now()))
            throw new CouponExpiredException("Can't create an expired coupon");
        //save coupon
        Coupon newCoupon = couponRepo.save(coupon);
        System.out.println("New coupon was added to " + companyRepo.findById(companyId).get().getName());
        return newCoupon;

    }

    //READ

    /**@param id the ID of the requested coupon
     * @return one of the companies coupons by a given ID
     * @throws NotFoundException coupon not found
     */
    public Coupon getCompanyCoupon(int id) throws NotFoundException {
        List<Coupon> companyCoupons = getCompanyCoupons();
        for (Coupon companyCoupon: companyCoupons) {
            if (companyCoupon.getId() == id)
                return couponRepo.findById(id).orElseThrow(()-> new NotFoundException("Coupon not found"));
        }
        throw new NotFoundException("Coupon not found");
    }

    /**@return list of all the company's coupons
     */
    public List<Coupon> getCompanyCoupons(){
        return couponRepo.findAllByCompanyId(companyId);
    }

    /**@param category the category by which to filter
     * @return list of all the company's coupons of a given category
     */
    public List<Coupon> getCompanyCoupons(Category category){
        return couponRepo.findByCompanyIdAndCategory(companyId, category);
    }

    /**@param max the maximum price for coupons to be returned
     * @return list of all company's coupons under a given price tag
     */
    public List<Coupon> getCompanyCoupons(double max){
        return couponRepo.findByPriceBetween(0, max);
    }

    /** @return company object with coupons
     * @throws NotFoundException company not found
     */
    public Company getCompanyDetail() throws NotFoundException {
        Company company = companyRepo.findById(companyId).orElseThrow( () -> new NotFoundException("Company not found"));
        company.setCoupons(couponRepo.findAllByCompanyId(companyId));
        return company;
    }

    //UPDATE

    /**checks if the coupon exists by comparing the id to the database, if found updates
     * @param coupon the coupon to be updated
     * @return coupon after the update
     * @throws NotFoundException could not find coupon
     */
    public Coupon updateCoupon(Coupon coupon) throws NotFoundException {
        if(!couponRepo.existsById(coupon.getId()))
            throw new NotFoundException("Coupon not found");
        couponRepo.save(coupon);
        System.out.println("Coupon was updated");
        return couponRepo.findById(coupon.getId()).get();
    }

    //DELETE

    /**checks if the coupon exists by comparing the id to the database, if found deletes the purchases of the coupon
     * then deletes it
     * @param couponId the ID of the coupon
     * @throws NotFoundException could not find coupon
     */
    public void deleteCoupon(int couponId) throws NotFoundException {
        if(!couponRepo.existsById(couponId))
            throw new NotFoundException("Coupon not found");
        couponRepo.deletePurchasesByCouponId(couponId);
        couponRepo.deleteById(couponId);
        System.out.println("Coupon no' " + couponId + " was deleted");
    }
}
